
package com.axonivy.connector.adobe.esign.connector.auth.oauth;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.connector.adobe.esign.connector.auth.oauth.OAuth2TokenRequester.AuthContext;
import com.axonivy.connector.adobe.esign.connector.enums.AdobeVariable;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;
import ch.ivyteam.ivy.request.IRequest;
import ch.ivyteam.ivy.rest.client.FeatureConfig;
import ch.ivyteam.ivy.rest.client.RestClientFactoryConstants;

public class OAuth2BearerFilter implements javax.ws.rs.client.ClientRequestFilter {
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";
	public static final String CODE_PARAM = "code";

	private final OAuth2TokenRequester getToken;
	private final OAuth2UriProperty uriFactory;

	private static final String TOKEN_SEPARATOR = ":";
	public static final String AUTH_SCOPE_PROPERTY = "AUTH.scope";

	public static final AdobeVariable REFRESH_TOKEN_VAR = AdobeVariable.OAUTH_TOKEN;
	public static final AdobeVariable ACCESS_TOKEN_VAR = AdobeVariable.ACCESS_TOKEN;
	
	private static final String OAUTH2_ERROR_CODE = "ivy:error:rest:client:oauth2";

	private String property;
	private Supplier<String> name = null;

	public OAuth2BearerFilter(OAuth2TokenRequester getToken, OAuth2UriProperty uriFactory) {
		this.getToken = getToken;
		this.uriFactory = uriFactory;
	}

	@Override
	public void filter(ClientRequestContext context) throws IOException {
		if (uriFactory.isAuthRequest(context.getUri())) { // already in token request: avoid stackOverflow
			return;
		}

		if (context.getHeaders().containsKey(AUTHORIZATION)) { // already set by other feature or explicit header
			return;
		}

		String accessToken = getAccessToken(context);
		context.getHeaders().add(AUTHORIZATION, BEARER + accessToken);
	}

	protected final String getAccessToken(ClientRequestContext context) {
		FeatureConfig config = new FeatureConfig(context.getConfiguration(), getSource());
		VarTokenStore refreshTokenStore = VarTokenStore.get(REFRESH_TOKEN_VAR.getVariableName());
		var refreshToken = refreshTokenStore.getToken();
		
		VarTokenStore accessTokenStore = VarTokenStore.get(ACCESS_TOKEN_VAR.getVariableName());
		var accessToken = accessTokenStore.getToken();
		
		String resultToken = null;
		
		if(accessToken == null || accessToken.isExpired()) {
			// refresh access token
			if(refreshToken != null && refreshToken.hasRefreshToken()) {
				accessToken = getRefreshedAccessToken(context.getClient(), config, refreshToken.refreshToken());
				accessTokenStore.setToken(accessToken);
				resultToken = accessToken.accessToken();
			}
			else { // get new access token
				refreshToken = getNewAccessToken(context.getClient(), config);
				refreshTokenStore.setToken(refreshToken);
				accessTokenStore.setToken(refreshToken);
				resultToken = refreshToken.accessToken();
			}
		}
		else { // use existing token
			resultToken = accessToken.accessToken();
		}
		
		if (accessToken != null && !accessToken.hasAccessToken()) {
			accessTokenStore.setToken(null);
			authError().withMessage("Failed to read 'access_token' from " + refreshToken).throwError();
		}

		return resultToken;
	}

	String createKey(FeatureConfig config) {
		Object clientId = config.readMandatory(RestClientFactoryConstants.PROPERTY_CLIENT_ID);
		var key = new StringBuilder((String) clientId);
		if (property != null) {
			key.append(TOKEN_SEPARATOR).append(config.readMandatory(property));
		} else {
			config.read(AUTH_SCOPE_PROPERTY).ifPresent(scope -> key.append(TOKEN_SEPARATOR).append(scope));
		}
		if (name != null) {
			key.append(TOKEN_SEPARATOR).append(name.get());
		}
		return key.toString();
	}

	private Class<?> getSource() {
		Class<?> type = getToken.getClass();
		Class<?> declaring = type.getDeclaringClass();
		if (declaring != null) {
			return declaring;
		}
		return type;
	}

	private Token getNewAccessToken(Client client, FeatureConfig config) {
		return getAccessToken(client, config, null);
	}

	private Token getRefreshedAccessToken(Client client, FeatureConfig config, String refreshToken) {
		return getAccessToken(client, config, refreshToken);
	}

	private Token getAccessToken(Client client, FeatureConfig config, String refreshToken) {
		GenericType<Map<String, Object>> map = new GenericType<>(Map.class);
		
		// use refresh uri for refresh token
		var tokenUri = StringUtils.isNotBlank(refreshToken) ? uriFactory.getRefreshUri() : uriFactory.getTokenUri();
		String authCode = getAuthCode();
		var authContext = new AuthContext(client.target(tokenUri), config, authCode, refreshToken);
		var response = getToken.requestToken(authContext);
		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			return new Token(response.readEntity(map));
		}
		throw authError().withMessage("Failed to get or refresh access token: " + response)
				.withAttribute("status", response.getStatus())
				.withAttribute("payload", response.readEntity(String.class)).build();
	}

	protected String getAuthCode() {
		var request = IRequest.current();
		if (request == null) {
			return null;
		}
		var authCode = request.getFirstParameter(CODE_PARAM);
		if (StringUtils.isBlank(authCode)) {
			return null;
		}
		return authCode;
	}

	private static BpmPublicErrorBuilder authError() {
		return BpmError.create(OAUTH2_ERROR_CODE);
	}
}
