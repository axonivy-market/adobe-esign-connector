package com.axonivy.connector.adobe.esign.connector.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.connector.adobe.esign.connector.rest.DownloadResult;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Utility class for JSF file handling e.g. to create StreamedContent
 *
 * @author jpl
 *
 */
public class FileUtils {

	public static StreamedContent toStreamedContent(String filename, DownloadResult download) throws FileNotFoundException {
		return DefaultStreamedContent.builder()
				.name(filename)
				.contentType(Constants.PDF_CONTENT_TYPE)
				.stream(() -> {
					try {
						return new FileInputStream(download.getFile().getJavaFile());
					} catch (FileNotFoundException e) {
						Ivy.log().warn(e.getMessage());
					}
					return null;
				})
				.build();
	}

	public static InputStream toInputStream(byte[] bytes) {
		if (bytes != null) {
			return new ByteArrayInputStream(bytes);
		}

		return new ByteArrayInputStream(new byte[0]);
	}
}
