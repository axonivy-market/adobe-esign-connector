package com.axonivy.connector.adobe.esign.connector.auth;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import ch.ivyteam.ivy.environment.Ivy;

public class JacksonUtils {
	private static final JaxRsClientJson JAXRS_CLIENT_JSON = new JaxRsClientJson();
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static JaxRsClientJson getJaxRsClientJsonProvider() {
		return JAXRS_CLIENT_JSON;
	}

	public static String writeObjectAsJson(Object entity) {
		try {
			return OBJECT_MAPPER.writeValueAsString(entity);
		} catch (JsonProcessingException e) {
			Ivy.log().warn(e.getMessage());
		}
		return null;
	}

	private static class JaxRsClientJson extends JacksonJsonProvider {
		@SuppressWarnings("deprecation")
		@Override
		public ObjectMapper locateMapper(Class<?> type, MediaType mediaType) {
			ObjectMapper mapper = super.locateMapper(type, mediaType);
			// match our generated jax-rs client beans: that contain JSR310 data types
			mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
			// allow fields starting with an upper case character (e.g. in ODATA specs)!
			mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
			// not sending this optional value seems to be lass prone to errors for some
			// remote services.
			mapper.setSerializationInclusion(Include.NON_NULL);
			return mapper;
		}
	}
}
