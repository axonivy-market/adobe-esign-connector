package com.axonivy.connector.adobe.esign.connector.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.Boundary;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;

import com.axonivy.connector.adobe.esign.connector.ui.util.Constants;

import api.rest.v6.client.TransientDocumentResponse;
import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.scripting.objects.File;

/**
 * REST client services to support download and download of attachments
 * 
 * @author jpl
 *
 */
public class ClientService {
	
	// error messages
	private static final String IO_ERROR = "unable to read bytes from REST service stream";
	private static final String IO_IN_ERROR = "unable to create file multipart for REST service";
	
	// Fallback content type, to be used when the conten't type can't be guessed from the file name
	private static final String FALLBACK_CONTENT_TYPE = "application/octet-stream";

	public static UploadResult upload(WebTarget target, String filename, byte[] bytes) {
		UploadResult result = new UploadResult();
		MultiPart multipart;

		try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
			FormDataContentDisposition formDataContentDisposition = FormDataContentDisposition.name("File")
			        .fileName(filename).build();
			MediaType fileMediaType = getContentType(filename);
			FormDataBodyPart filePart = new FormDataBodyPart(formDataContentDisposition, bytes, fileMediaType);
			
			multipart = formDataMultiPart
				.field("File-Name", filename, MediaType.TEXT_PLAIN_TYPE)
				.field("Mime-Type", fileMediaType.toString())
				.bodyPart(filePart);
			multipart.bodyPart(filePart);
			
			MediaType contentType = MediaType.MULTIPART_FORM_DATA_TYPE;
			contentType = Boundary.addBoundary(contentType);
			
			Response response = target.request().post(Entity.entity(multipart, contentType));
			
			if(response.getStatus() == Response.Status.OK.getStatusCode() || 
					response.getStatus() == Response.Status.CREATED.getStatusCode()) {
				TransientDocumentResponse entity = response.readEntity(TransientDocumentResponse.class);
				result.setId(entity.getTransientDocumentId());
			} else {
				result.setError(BpmError.create(Constants.ERROR_BASE)
						.withCause(new WebApplicationException("Http Call failed. response code is " + response.getStatus() +
								". Error reported is " + response.getStatusInfo()))
						.build());
			}
		} catch (IOException e) {
			result.setError(BpmError.create(Constants.ERROR_BASE)
					.withMessage(IO_IN_ERROR)
					.withCause(e)
					.build());
		}

		return result;
	}
	
	/**
	 * Gets the response from the provided {@link WebTarget}, creates {@link DownloadResult} and returns it
	 * @param target
	 * @param filename
	 * @param asFile
	 * @return
	 */
	public static DownloadResult download(WebTarget target, String filename, boolean asFile) {
		DownloadResult result = new DownloadResult();
		try(Response response = target.request().get()) {
			result = download(response, filename, asFile);
		}
		
		return result;
	}
	
	/***
	 * Creates {@link DownloadResult} from the provided {@link Response}
	 * @param response
	 * @param filename
	 * @param asFile
	 * @return
	 */
	public static DownloadResult download(Response response, String filename, boolean asFile) {
		DownloadResult result = new DownloadResult();
		
		if(response.getStatus() == Response.Status.OK.getStatusCode()) {
			try(InputStream is = response.readEntity(InputStream.class)) {
				byte[] bytes = IOUtils.toByteArray(is);
				result.setFilename(filename);
				if(asFile) {
					File file = new File(filename, true);
					FileUtils.writeByteArrayToFile(file .getJavaFile(), bytes);
					result.setFile(file);
				} else {
					result.setContent(bytes);
				}
			} catch (IOException e) {
				result.setError(BpmError.create(Constants.ERROR_BASE)
						.withMessage(IO_ERROR)
						.withCause(e).build());
			}
		}
		
		return result;
	}
	
	private static MediaType getContentType(String filename) throws IOException {
		String contentType = URLConnection.guessContentTypeFromName(filename);

		if (StringUtils.isBlank(contentType)) {
			contentType = FALLBACK_CONTENT_TYPE;
		}
		
		return MediaType.valueOf(contentType);
	}

}
