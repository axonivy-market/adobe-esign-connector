package com.axonivy.connector.adobe.esign.connector.test;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.rest.client.RestClient;
import ch.ivyteam.ivy.rest.client.RestClient.Builder;
import ch.ivyteam.ivy.rest.client.RestClients;

@IvyProcessTest(enableWebServer = true)
public class TestAdobeSignConnector {
	protected static final String TRANSIENT_DOCUMENTS = "TransientDocuments";
	protected static final String AGREEMENTS = "Agreements";

	protected void prepareRestClient(IApplication app, AppFixture fixture, String clientName) {
		fixture.var("adobe-sign-connector.host", "TESTHOST");
		fixture.var("adobe-sign-connector.integrationKey", "TESTUSER");
		RestClient restClient = RestClients.of(app).find(clientName);
		// change created client: use test url and a slightly different version of
		// the
		// DocuWare Auth feature
		Builder builder = RestClient.create(restClient.name()).uuid(restClient.uniqueId())
				.uri("http://{ivy.engine.host}:{ivy.engine.http.port}/{ivy.request.application}/api/adobeSignMock")
				.description(restClient.description()).properties(restClient.properties());

		for (String feature : restClient.features()) {
			builder.feature(feature);
		}

		builder.feature("ch.ivyteam.ivy.rest.client.security.CsrfHeaderFeature");
		restClient = builder.toRestClient();
		RestClients.of(app).set(restClient);
	}
}
