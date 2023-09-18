package com.axonivy.connector.adobe.esign.connector.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import ch.ivyteam.api.API;
import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@PermitAll
@Path("adobeSignMock")
public class AdobeSignAgreementsServiceMock {

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("agreements")
	public Response createAgreement(String payload) {
		API.checkParameterNotNull(payload, "payload");
		return Response.status(201).entity(load("json/createAgreement.json")).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("agreements/{agreementId}/documents")
	public Response getDocuments(@PathParam(value = "agreementId") String agreementId) {
		API.checkParameterNotNull(agreementId, "agreementId");
		return Response.status(201).entity(load("json/getDocuments.json")).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("agreements/{agreementId}/documents/{documentId}")
	public Response downloadDocument(@PathParam(value = "agreementId") String agreementId,
			@PathParam(value = "documentId") String documentId) throws IOException {
		API.checkParameterNotNull(agreementId, "agreementId");
		return Response.status(200).entity(TestService.getSamplePdf()).build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("agreements/{agreementId}/signingUrls")
	public Response getSigningUrls(@PathParam(value = "agreementId") String agreementId) {
		API.checkParameterNotNull(agreementId, "agreementId");
		return Response.status(201).entity(load("json/getSigningUrls.json")).build();
	}

	private static String load(String path) {
		try (InputStream is = AdobeSignAgreementsServiceMock.class.getResourceAsStream(path)) {
			return IOUtils.toString(is, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read resource: " + path);
		}
	}
}
