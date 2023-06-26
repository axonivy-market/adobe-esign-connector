package com.axonivy.connector.adobe.esign.connector.rest;

import java.util.Optional;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * REST service for Adobe Sign WebHook communication. This service provides methods for verification and to receive notifications.
 * <br/>
 * The service needs to be configured as WebHook in the Adobe Sign profile settings.
 * @see <a href="https://opensource.adobe.com/acrobat-sign/acrobat_sign_events/index.html#creating-a-webhook">WebHooks in Adobe Sign</a>
 * 
 * @author jpl
 *
 */
@Path("/adobe/webhook")
@PermitAll
public class WebHookService {

	private static final String CLIENT_ID_HEADER = "X-ADOBESIGN-CLIENTID";
	private static final String CLIENT_ID_VAR = "ivy.var.adobe-sign-connector.clientId";

	@GET
	@Produces("application/json")
	public Response verification(@Context HttpHeaders headers) {
		Response response;
		String clientId = headers.getHeaderString(CLIENT_ID_HEADER);
		if(StringUtils.isNotBlank(clientId)) {
			response = Response.accepted().entity(new ClientIdResponse(clientId)).build();
		} else {
			response = Response.status(Status.NOT_ACCEPTABLE).build();
		}
		
		return response;
	}
	
	@POST
	@Consumes("application/json")
	public void notification(JsonNode node) {
		Ivy.log().info(node);
	}
	
	private boolean verifyClientId(String clientId) {
		return Optional.ofNullable(Ivy.var().get(CLIENT_ID_VAR)).map(s -> s.compareTo(clientId) == 0).orElse(false);
	}
	
	private record ClientIdResponse(String xAdobeSignClientId) {};
	
}
