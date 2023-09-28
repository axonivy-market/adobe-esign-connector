package com.axonivy.connector.adobe.esign.connector.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.primefaces.PrimeFaces;

import com.axonivy.connector.adobe.esign.connector.AdminSetup.AdminSetupData;
import com.axonivy.connector.adobe.esign.connector.auth.OAuth2Feature.AccessTokenRequest;
import com.axonivy.connector.adobe.esign.connector.auth.oauth.OAuth2UriProperty;
import com.axonivy.connector.adobe.esign.connector.auth.oauth.Token;
import com.axonivy.connector.adobe.esign.connector.auth.oauth.VarTokenStore;
import com.axonivy.connector.adobe.esign.connector.enums.AdobeVariable;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.request.EngineUriResolver;

public class AdminSetupService {
	private static final String OAUTH_RESUME_START_FRIENDLY_PATH = "Setup/AdminSetup/oauthResume.ivp";

	/**
	 * Initializes variables for the dialog data
	 * @param data dialog data
	 */
	public static void initVars(AdminSetupData data) {
		data.setAuthenticationUri(AdobeVariable.AUTHENTICATION_URI.getValue());
		data.setBaseUri(AdobeVariable.BASE_URI.getValue());
		data.setClientId(AdobeVariable.CLIENT_ID.getValue());
		data.setClientSecret(AdobeVariable.CLIENT_SECRET.getValue());
		data.setHost(AdobeVariable.HOST.getValue());
		data.setIntegrationKey(AdobeVariable.INTEGRATION_KEY.getValue());
		data.setOauthToken(AdobeVariable.OAUTH_TOKEN.getValue());
		data.setPermissions(AdobeVariable.PERMISSIONS.getValue());
		data.setReturnPage(AdobeVariable.RETURN_PAGE.getValue());
		data.setAccessToken(AdobeVariable.ACCESS_TOKEN.getValue());
	}

	/**
	 * Updates Variables from dialog data
	 * @param data dialog data
	 */
	public static void updateVars(AdminSetupData data) {
		AdobeVariable.AUTHENTICATION_URI.updateValue(data.getAuthenticationUri());
		AdobeVariable.BASE_URI.updateValue(data.getBaseUri());
		AdobeVariable.CLIENT_ID.updateValue(data.getClientId());
		AdobeVariable.CLIENT_SECRET.updateValue(data.getClientSecret());
		AdobeVariable.HOST.updateValue(data.getHost());
		AdobeVariable.INTEGRATION_KEY.updateValue(data.getIntegrationKey());
		AdobeVariable.PERMISSIONS.updateValue(data.getPermissions());
		AdobeVariable.RETURN_PAGE.updateValue(data.getReturnPage());
		// dont update oauth token variables
	}

	/**
	 * Opens Adobe OAuth authentication page with parameters from Variables
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws UriBuilderException
	 * @throws URISyntaxException
	 */
	public static void authRedirect()
			throws IOException, IllegalArgumentException, UriBuilderException, URISyntaxException {
		var clientId = AdobeVariable.CLIENT_ID.getValue();
		var scope = AdobeVariable.PERMISSIONS.getValue();
		String redirectUri = createRedirectUrl();

		String authUri = AdobeVariable.AUTHENTICATION_URI.getValue();
		URI uri = UriBuilder.fromUri(new URI(authUri))
				.queryParam("redirect_uri", redirectUri)
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("scope", scope)
				.build();
		PrimeFaces.current().executeScript("window.open('" + uri.toString() + "', '_top')");
	}

	/**
	 * Tries to get the OAuth access token with given code and parameters loaded.
	 * Stores the token in the Variable {@link AdobeVariable#OAUTH_TOKEN}
	 * 
	 * @param authCode
	 * @throws URISyntaxException
	 */
	public static void getNewAccessToken(String authCode) throws URISyntaxException {
		var clientId = AdobeVariable.CLIENT_ID.getValue();
		var clientSecret = AdobeVariable.CLIENT_SECRET.getValue();
		UriBuilder tokenBaseUri = UriBuilder.fromUri(new URI(AdobeVariable.BASE_URI.getValue()));

		// get token stores for refresh and access tokens
		VarTokenStore refreshTokenStore = VarTokenStore.get(AdobeVariable.OAUTH_TOKEN.getVariableName());
		VarTokenStore accessTokenStore = VarTokenStore.get(AdobeVariable.ACCESS_TOKEN.getVariableName());

		//request new token
		Client client = ClientBuilder.newClient();
		tokenBaseUri.path(OAuth2UriProperty.TOKEN_RELATIVE_PATH);
		AccessTokenRequest authRequest = new AccessTokenRequest(authCode, clientId, clientSecret,
				createRedirectUrl());
		WebTarget target = client.target(tokenBaseUri);
		Response response = target.request().post(Entity.form(authRequest.paramsMap()));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			GenericType<Map<String, Object>> map = new GenericType<>(Map.class);
			// store new token
			Token newToken = new Token(response.readEntity(map));
			refreshTokenStore.setToken(newToken);
			accessTokenStore.setToken(newToken);
		} else {
			Ivy.log().error("failed to get access token: " + response);
		}
	}

	/**
	 * Creates URI for redirecting after Adobe authentication
	 * 
	 * @return
	 */
	public static String createRedirectUrl() {
		String startUri = ProcessStartService
				.findRelativeUrlByProcessStartFriendlyRequestPath(OAUTH_RESUME_START_FRIENDLY_PATH);
		URI redirectUri = UriBuilder.fromUri(EngineUriResolver.instance().external()).path(startUri).build();
		return redirectUri.toString();
	}
}
