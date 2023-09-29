package com.axonivy.connector.adobe.esign.connector.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import ch.ivyteam.api.API;
import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@PermitAll
@Path("adobeSignMock")
public class AdobeSignTransientDocumentsServiceMock {

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("transientDocuments")
  public Response transientDocuemts(String payload) {
    API.checkParameterNotNull(payload, "payload");
    return Response.status(201)
            .entity(load("json/uploadDocument.json"))
            .build();
  }

  private static String load(String path) {
    try (InputStream is = AdobeSignTransientDocumentsServiceMock.class.getResourceAsStream(path)) {
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to read resource: " + path);
    }
  }
}
