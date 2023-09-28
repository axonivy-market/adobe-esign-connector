package com.axonivy.connector.adobe.esign.connector.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.axonivy.connector.adobe.esign.connector.TransientDocumentsData;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.security.ISession;

@IvyProcessTest
public class TestTransientDocumentsService extends TestAdobeSignConnector {
	
	
	 private static final BpmElement testeeUploadDocument = BpmProcess.path("connector/TransientDocuments")
	          .elementName("uploadDocument(file)");

	@Test
	public void uploadFile(BpmClient bpmClient, ISession session, AppFixture fixture, IApplication app)
			throws IOException {
		prepareRestClient(app, fixture, TRANSIENT_DOCUMENTS);
		
		java.io.File pdf = TestService.getSamplePdf();
		assertThat(pdf).isNotNull();
		
		ExecutionResult result = bpmClient.start().subProcess(testeeUploadDocument).withParam("file", pdf).execute();
		TransientDocumentsData data = result.data().last();
		assertThat(data.getId()).isNotEmpty();
	}
}
