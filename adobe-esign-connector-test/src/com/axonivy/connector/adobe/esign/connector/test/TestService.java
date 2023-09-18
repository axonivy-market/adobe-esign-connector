package com.axonivy.connector.adobe.esign.connector.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import org.apache.commons.compress.utils.IOUtils;

import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.File;

public class TestService {
	private static final String SAMPLE_PDF_CMS_PATH = "/Files/samplePdf";

	public static java.io.File getSamplePdf() throws IOException {
		ContentObjectValue cov = Ivy.cm().findValue(SAMPLE_PDF_CMS_PATH).resolve(Locale.ENGLISH).orElse(null);
		if (cov == null) {
			return null;
		}
		java.io.File sampleFile = new File("/samplePdf.pdf", true).getJavaFile();
		FileOutputStream fos = new FileOutputStream(sampleFile);
		IOUtils.copy(cov.read().inputStream(), fos);
		return sampleFile;

	}
}
