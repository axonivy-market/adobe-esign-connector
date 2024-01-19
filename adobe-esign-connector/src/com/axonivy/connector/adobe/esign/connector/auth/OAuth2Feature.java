package com.axonivy.connector.adobe.esign.connector.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.connector.adobe.esign.connector.auth.oauth.OAuth2BearerFilter;
import com.axonivy.connector.adobe.esign.connector.auth.oauth.OAuth2TokenRequester.AuthContext;
import com.axonivy.connector.adobe.esign.connector.auth.oauth.OAuth2UriProperty;
import com.axonivy.connector.adobe.esign.connector.enums.AdobeVariable;
import com.axonivy.connector.adobe.esign.connector.util.Constants;

import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.rest.client.FeatureConfig;
import ch.ivyteam.ivy.rest.client.oauth2.OAuth2RedirectErrorBuilder;
import ch.ivyteam.ivy.rest.client.oauth2.uri.OAuth2CallbackUriBuilder;

public class OAuth2Feature implements Feature {

	public static interface Property {

		String CLIENT_ID = "AUTH.clientId";
		String CLIENT_SECRET = "AUTH.clientSecret";
		String AUTHORIZATION_CODE = "AUTH.code";
		String SCOPE = "AUTH.scope";
		String AUTH_BASE_URI = "AUTH.baseUri";
		String AUTH_INTEGRATION_KEY = "AUTH.integrationKey";
	}

	public static final String SESSION_TOKEN = "adobe.esign.authCode";

	@Override
	public boolean configure(FeatureContext context) {
		var config = new FeatureConfig(context.getConfiguration(), OAuth2Feature.class);
		String intKey = config.read(Property.AUTH_INTEGRATION_KEY).orElse("");

		// use oauth if integration key is blank, property comes as defined (${ivy.var
		// ...) if the variable is not defined
		if (StringUtils.isBlank(intKey) || intKey.startsWith("${ivy.var")) {
			var adobeSignUri = new OAuth2UriProperty(config, Property.AUTH_BASE_URI,
					Ivy.var().get(AdobeVariable.BASE_URI.getVariableName()));
			var oauth2 = new OAuth2BearerFilter(ctxt -> requestToken(ctxt, adobeSignUri), adobeSignUri);
			context.register(oauth2, Priorities.AUTHORIZATION);
		} else {
			context.register(BearerTokenAuthorizationFilter.class, Priorities.AUTHORIZATION - 10);
		}

		return true;
	}

	/**
	 * Get token.
	 *
	 * @param ctxt
	 * @param uriFactory
	 */
	private static Response requestToken(AuthContext ctxt, OAuth2UriProperty uriFactory) {
		String authCode = ctxt.authCode().orElse("");
		var refreshToken = ctxt.refreshToken();
		if (authCode.isEmpty() && refreshToken.isEmpty()) {
			try {
				authRedirectError(ctxt.config, uriFactory).throwError();
			} catch (IllegalArgumentException | UriBuilderException | URISyntaxException e) {
				Ivy.log().error(e.getMessage());
			}
		}
		var clientId = ctxt.config.readMandatory(Property.CLIENT_ID);
		var clientSecret = ctxt.config.readMandatory(Property.CLIENT_SECRET);
		Response response = null;
		if (!refreshToken.isPresent()) {
			AccessTokenRequest authRequest = new AccessTokenRequest(authCode, clientId, clientSecret,
					OAuth2CallbackUriBuilder.create().toUrl().toString());
			response = ctxt.target.request().post(Entity.form(authRequest.paramsMap()));
		} else {
			RefreshTokenRequest authRequest = new RefreshTokenRequest(refreshToken.get(), clientId, clientSecret);
			response = ctxt.target.request().post(Entity.form(authRequest.paramsMap()));
		}
		return response;
	}

	/**
	 * Use the JWT grant?
	 *
	 * @param use
	 */
	public static boolean isTrue(Optional<String> use) {
		return use.filter(val -> !val.isBlank() && Boolean.parseBoolean(val)).isPresent();
	}

	public static class AccessTokenRequest {

		public String grantType;
		public String clientId;
		public String clientSecret;
		public String redirectUri;
		public String code;

		public AccessTokenRequest(String code, String clientId, String clientSecret, String redirectUri) {
			this.grantType = Constants.AUTHORIZATION_CODE;
			this.clientId = clientId;
			this.clientSecret = clientSecret;
			this.redirectUri = redirectUri;
			this.code = code;
		}

		public MultivaluedMap<String, String> paramsMap() {
			MultivaluedMap<String, String> values = new MultivaluedHashMap<>();
			values.put(Constants.ACCESS_TOKEN_REQUEST_CODE, Arrays.asList(code));
			values.put(Constants.ACCESS_TOKEN_REQUEST_CLIENT_ID, Arrays.asList(clientId));
			values.put(Constants.ACCESS_TOKEN_REQUEST_CLIENT_SECRET, Arrays.asList(clientSecret));
			values.put(Constants.ACCESS_TOKEN_REQUEST_REDIRECT_URI, Arrays.asList(redirectUri));
			values.put(Constants.ACCESS_TOKEN_REQUEST_GRANT_TYPE, Arrays.asList(grantType));
			return values;
		}

		@Override
		public String toString() {
			return String.format("""
					code=%s
					client_id=%s&
					client_secret=%s&
					redirect_uri=%s
					grant_type=%s
					""", code, clientId, clientSecret, redirectUri, grantType);
		}

	}

	public static class RefreshTokenRequest {

		public String grantType;
		public String refreshToken;
		public String clientId;
		public String clientSecret;

		public RefreshTokenRequest(String refreshToken, String clientId, String clientSecret) {
			this.grantType = Constants.REFRESH_TOKEN;
			this.refreshToken = refreshToken;
			this.clientId = clientId;
			this.clientSecret = clientSecret;
		}

		public MultivaluedMap<String, String> paramsMap() {
			MultivaluedMap<String, String> values = new MultivaluedHashMap<>();
			values.put(Constants.ACCESS_TOKEN_REQUEST_CLIENT_ID, Arrays.asList(clientId));
			values.put(Constants.ACCESS_TOKEN_REQUEST_CLIENT_SECRET, Arrays.asList(clientSecret));
			values.put(Constants.REFRESH_TOKEN, Arrays.asList(refreshToken));
			values.put(Constants.ACCESS_TOKEN_REQUEST_GRANT_TYPE, Arrays.asList(grantType));
			return values;
		}

		@Override
		public String toString() {
			return String.format("""
					client_id=%s&
					client_secret=%s&
					refresh_token=%s
					grant_type=%s
					""", clientId, clientSecret, refreshToken, grantType);
		}
	}

	private static BpmPublicErrorBuilder authRedirectError(FeatureConfig config, OAuth2UriProperty uriFactory)
			throws IllegalArgumentException, UriBuilderException, URISyntaxException {
		URI redirectUri = OAuth2CallbackUriBuilder.create().toUrl();
		String authUri = AdobeVariable.AUTHENTICATION_URI.getValue();
		var uri = UriBuilder.fromUri(new URI(authUri))
				.queryParam(Constants.ACCESS_TOKEN_REQUEST_REDIRECT_URI, redirectUri)
				.queryParam(Constants.RESPONSE_TYPE, Constants.ACCESS_TOKEN_REQUEST_CODE)
				.queryParam(Constants.ACCESS_TOKEN_REQUEST_CLIENT_ID, config.readMandatory(Property.CLIENT_ID))
				.queryParam(Constants.SCOPE, getScope(config)).build();
		return OAuth2RedirectErrorBuilder.create(uri).withMessage("Missing permission from user to act in his name.");
	}

	static String getScope(FeatureConfig config) {
		return config.read(Property.SCOPE).orElse("signature impersonation");
	}

}