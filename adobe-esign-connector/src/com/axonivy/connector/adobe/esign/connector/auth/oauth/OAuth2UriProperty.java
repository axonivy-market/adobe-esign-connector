package com.axonivy.connector.adobe.esign.connector.auth.oauth;

import java.net.URI;

import ch.ivyteam.ivy.rest.client.FeatureConfig;

public class OAuth2UriProperty extends ch.ivyteam.ivy.rest.client.oauth2.uri.OAuth2UriProperty {
	
	public static final String TOKEN_RELATIVE_PATH = "token";
	public static final String REFRESH_RELATIVE_PATH = "refresh";


	public OAuth2UriProperty(FeatureConfig config, String defaultBaseUri) {
		super(config, defaultBaseUri);
	}
	
	public OAuth2UriProperty(FeatureConfig config, String baseUriPropertyName, String defaultBaseUri) {
	    super(config, baseUriPropertyName, defaultBaseUri);
	  }
	
	public URI getRefreshUri() {
		return getUri(REFRESH_RELATIVE_PATH);
	}

	@Override
	public URI getTokenUri() {
		return getUri(TOKEN_RELATIVE_PATH);
	}
}
