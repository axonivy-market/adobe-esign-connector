package com.axonivy.connector.adobe.esign.connector.auth;

import java.net.URI;

import ch.ivyteam.ivy.rest.client.FeatureConfig;

public class OAuth2UriProperty extends ch.ivyteam.ivy.rest.client.oauth2.uri.OAuth2UriProperty {

	public OAuth2UriProperty(FeatureConfig config, String defaultBaseUri) {
		super(config, defaultBaseUri);
	}
	
	public OAuth2UriProperty(FeatureConfig config, String baseUriPropertyName, String defaultBaseUri) {
	    super(config, baseUriPropertyName, defaultBaseUri);
	  }
	
	public URI getRefreshUri() {
		return getUri("refresh");
	}

}
