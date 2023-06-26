package com.axonivy.connector.adobe.esign.connector.auth;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.apache.commons.lang.StringUtils;

import ch.ivyteam.ivy.rest.client.FeatureConfig;

public class BearerTokenAuthorizationFilter implements ClientRequestFilter {
	
	private static final String INTEGRATIONKEY = "AUTH.integrationKey";
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		FeatureConfig config = new FeatureConfig(requestContext.getConfiguration(), BearerTokenAuthorizationFilter.class);
		String integrationKey = config.readMandatory(INTEGRATIONKEY);
		if(StringUtils.isNotBlank(integrationKey)) {
			requestContext.getHeaders().add(AUTHORIZATION, BEARER + integrationKey);
		}
	}

}
