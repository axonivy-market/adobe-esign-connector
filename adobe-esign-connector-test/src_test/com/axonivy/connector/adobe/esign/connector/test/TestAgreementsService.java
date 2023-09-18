package com.axonivy.connector.adobe.esign.connector.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.connector.adobe.esign.connector.AgreementsData;
import com.axonivy.connector.adobe.esign.connector.rest.DownloadResult;
import com.axonivy.connector.adobe.esign.connector.service.AdobeSignService;

import api.rest.v6.client.AgreementCreationInfo;
import api.rest.v6.client.AgreementCreationInfo.SignatureTypeEnum;
import api.rest.v6.client.AgreementCreationInfo.StateEnum;
import api.rest.v6.client.AgreementCreationInfoParticipantSetsInfo.RoleEnum;
import api.rest.v6.client.AgreementCreationResponse;
import api.rest.v6.client.AgreementDocuments;
import api.rest.v6.client.SigningUrlResponseSigningUrlSetInfos;
import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.ISession;

@IvyProcessTest
public class TestAgreementsService extends TestAdobeSignConnector {

	private static final BpmElement testeeCreateAgreement = BpmProcess.path("connector/Agreements")
			.elementName("createAgreement(AgreementCreationInfo)");

	private static final BpmElement testeeGetDocuments = BpmProcess.path("connector/Agreements")
			.elementName("getDocuments(String)");

	private static final BpmElement testeeDownloadDocument = BpmProcess.path("connector/Agreements")
			.elementName("dowloadDocument(String, String, String, Boolean)");

	private static final BpmElement testeeGetSigningUrls = BpmProcess.path("connector/Agreements")
			.elementName("getSigningURLs(String,String)");

	@Test
	public void createAgreement(BpmClient bpmClient, ISession session, AppFixture fixture, IApplication app)
			throws IOException {

		prepareRestClient(app, fixture, AGREEMENTS);

		AgreementCreationInfo agreement = createTestAgreement();

		ExecutionResult result = bpmClient.start().subProcess(testeeCreateAgreement).withParam("agreement", agreement)
				.execute();
		AgreementsData data = result.data().last();
		AgreementCreationResponse response = data.getAgreementCreationResponse();
		assertThat(response).isNotNull();
		assertThat(response.getId()).isNotEmpty();
	}

	@Test
	public void getDocuments(BpmClient bpmClient, ISession session, AppFixture fixture, IApplication app)
			throws IOException {

		prepareRestClient(app, fixture, AGREEMENTS);

		String agreementId = "test-agreement-id";

		ExecutionResult result = bpmClient.start().subProcess(testeeGetDocuments).withParam("agreementId", agreementId)
				.execute();
		AgreementsData data = result.data().last();
		AgreementDocuments response = data.getDocuments();
		assertThat(response).isNotNull();
		assertThat(response.getDocuments()).isNotEmpty();
	}

	@Test
	public void downloadDocument(BpmClient bpmClient, ISession session, AppFixture fixture, IApplication app)
			throws IOException {

		prepareRestClient(app, fixture, AGREEMENTS);

		String agreementId = "test-agreement-id";
		String documentId = "test-document-id";
		String filename = "sample.pdf";
		Boolean asFile = Boolean.TRUE;

		ExecutionResult result = bpmClient.start().subProcess(testeeDownloadDocument)
				.withParam("agreementId", agreementId).withParam("documentId", documentId)
				.withParam("filename", filename).withParam("asFile", asFile).execute();
		AgreementsData data = result.data().last();
		DownloadResult downloadResult = data.getDownload();
		assertThat(downloadResult).isNotNull();
		if (downloadResult.getError() != null) {
			Ivy.log().error(downloadResult.getError());
		}
		assertThat(downloadResult.getFile()).isNotNull();
	}

	@Test
	public void getSigningUrls(BpmClient bpmClient, ISession session, AppFixture fixture, IApplication app)
			throws IOException {

		prepareRestClient(app, fixture, AGREEMENTS);

		String agreementId = "test-agreement-id";
		String frameParent = "test";

		ExecutionResult result = bpmClient.start().subProcess(testeeGetSigningUrls)
				.withParam("agreementId", agreementId).withParam("frameParent", frameParent).execute();
		AgreementsData data = result.data().last();
		
		List<SigningUrlResponseSigningUrlSetInfos> signingUrls = data.getSigningUrls();

		assertThat(signingUrls).isNotNull();
		assertThat(signingUrls).isNotEmpty();
	}

	private AgreementCreationInfo createTestAgreement() {
		AgreementCreationInfo agreement = new AgreementCreationInfo();

		agreement.setName("test name");
		agreement.setMessage("Please sign this document!");
		agreement.setSignatureType(SignatureTypeEnum.ESIGN);
		agreement.setState(StateEnum.IN_PROCESS);

		// add signers
		AdobeSignService.getInstance().createParticipantInfoForEmail(Arrays.asList("testEmail@test.test"),
				RoleEnum.SIGNER);
		agreement.setParticipantSetsInfo(AdobeSignService.getInstance()
				.createParticipantInfoForEmail(Arrays.asList("testEmail@test.test"), RoleEnum.SIGNER));

		// add documentIds - need to be already transferred with the upload document
		// service
		agreement.setFileInfos(
				AdobeSignService.getInstance().createFileInfosForDocumentIds(Arrays.asList("test-document-id")));

		agreement.setEmailOption(AdobeSignService.getInstance().createAllDisabledSendOptions());
		return agreement;
	}
}
