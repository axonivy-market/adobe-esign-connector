package com.axonivy.connector.adobe.esign.connector.json;

import javax.ws.rs.Priorities;
import javax.ws.rs.core.FeatureContext;

import com.axonivy.connector.adobe.esign.connector.auth.JacksonUtils;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import ch.ivyteam.ivy.rest.client.mapper.JsonFeature;

/**
 * JSON object mapper that complies with generated JAX-RS client pojos.
 *
 * @since 9.2
 */
public class OpenApiJsonFeature extends JsonFeature {
	@Override
	public boolean configure(FeatureContext context) {
		JacksonJsonProvider provider = JacksonUtils.getJaxRsClientJsonProvider();
		configure(provider, context.getConfiguration());
		context.register(provider, Priorities.ENTITY_CODER);
		return true;
	}
}
