package com.axonivy.connector.adobe.esign.connector.auth;

import java.net.URI;
import java.util.Optional;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.rest.client.FeatureConfig;
import ch.ivyteam.ivy.rest.client.oauth2.OAuth2BearerFilter;
import ch.ivyteam.ivy.rest.client.oauth2.OAuth2RedirectErrorBuilder;
import ch.ivyteam.ivy.rest.client.oauth2.OAuth2TokenRequester.AuthContext;
import ch.ivyteam.ivy.rest.client.oauth2.uri.OAuth2CallbackUriBuilder;

/**
 * @since 9.2
 */
public class OAuth2Feature implements Feature {

  public static interface Property {

    String CLIENT_ID = "AUTH.clientId";
    String CLIENT_SECRET = "AUTH.clientSecret";
    String AUTHORIZATION_CODE = "AUTH.code";
    String SCOPE = "AUTH.scope";
    String AUTH_BASE_URI = "AUTH.baseUri";
  }

  public static final String SESSION_TOKEN = "adobe.esign.authCode";

  @Override
  public boolean configure(FeatureContext context) {
    var config = new FeatureConfig(context.getConfiguration(), OAuth2Feature.class);
    var docuSignUri = new OAuth2UriProperty(config, Property.AUTH_BASE_URI,
            "https://api.eu2.echosign.com/oauth/v2");
    var oauth2 = new OAuth2BearerFilter(
            ctxt -> requestToken(ctxt, docuSignUri),
            docuSignUri);
    context.register(BearerTokenAuthorizationFilter.class, Priorities.AUTHORIZATION - 10);
    context.register(oauth2, Priorities.AUTHORIZATION);
    return true;
  }

  /**
   * Get token.
   *
   * @param ctxt
   * @param uriFactory
   */
  private static Response requestToken(AuthContext ctxt, OAuth2UriProperty uriFactory) {
	  String authCode = ctxt.config.readMandatory(Property.AUTHORIZATION_CODE);
	  var refreshToken = ctxt.refreshToken();
	  if (authCode.isEmpty() && refreshToken.isEmpty()) {
		  authRedirectError(ctxt.config, uriFactory).throwError();
	  }
	  var clientId = ctxt.config.readMandatory(Property.CLIENT_ID);
	  var clientSecret = ctxt.config.readMandatory(Property.CLIENT_SECRET);
	  URI tokenUri = uriFactory.getTokenUri();
	  Object authRequest;
	  if (!refreshToken.isPresent()) {
		  authRequest = new AccessTokenRequest(authCode, clientId, clientSecret, OAuth2CallbackUriBuilder.create().toUrl().toString());
	  } else {
		  authRequest = new RefreshTokenRequest(refreshToken.get(), clientId, clientSecret);
		  tokenUri = uriFactory.getRefreshUri();
	  }
	  var response = ctxt.target
			  .request()
			  .post(Entity.json(authRequest));
	  return response;
  }

  /**
   * Use the JWT grant?
   * @param use
   */
  public static boolean isTrue(Optional<String> use) {
    return use.filter(val -> !val.isBlank() && Boolean.parseBoolean(val)).isPresent();
  }

  public static class AccessTokenRequest {

    public String grant_type;
    public String client_id;
    public String client_secret;
    public String redirect_uri;
    public String code;

    public AccessTokenRequest(String code, String client_id, String client_secret, String redirect_uri) {
      this.grant_type = "authorization_code";
      this.client_id = client_id;
      this.client_secret = client_secret;
      this.redirect_uri = redirect_uri;
      this.code = code;
    }
  }

  public static class RefreshTokenRequest {

    public String grant_type;
    public String refresh_token;
    public String client_id;
    public String client_secret;

    public RefreshTokenRequest(String refreshToken, String client_id, String client_secret) {
      this.grant_type = "refresh_token";
      this.refresh_token = refreshToken;
      this.client_id = client_id;
      this.client_secret = client_secret;
    }
  }

  private static BpmPublicErrorBuilder authRedirectError(FeatureConfig config, OAuth2UriProperty uriFactory) {
    URI redirectUri = OAuth2CallbackUriBuilder.create().toUrl();
    var uri = UriBuilder.fromUri(uriFactory.getUri("auth"))
            .queryParam("response_type", "code")
            .queryParam("scope", getScope(config))
            .queryParam("client_id", config.readMandatory(Property.CLIENT_ID))
            .queryParam("redirect_uri", redirectUri)
            .build();
    Ivy.log().debug("created oauth URI: " + uri);
    return OAuth2RedirectErrorBuilder
            .create(uri)
            .withMessage("Missing permission from user to act in his name.");
  }

  static String getScope(FeatureConfig config) {
    return config.read(Property.SCOPE).orElse("signature impersonation");
  }
  
}