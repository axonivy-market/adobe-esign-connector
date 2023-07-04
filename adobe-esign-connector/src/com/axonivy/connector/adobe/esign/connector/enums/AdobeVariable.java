package com.axonivy.connector.adobe.esign.connector.enums;

public enum AdobeVariable {
	BASE_URI("adobe-sign-connector.baseUri"), HOST("adobe-sign-connector.host"),
	INTEGRATION_KEY("adobe-sign-connector.integrationKey"), RETURN_PAGE("adobe-sign-connector.returnPage"),
	PERMISSIONS("adobe-sign-connector.permissions"), CLIENT_ID("adobe-sign-connector.clientId"),
	CLIENT_SECRET("adobe-sign-connector.clientSecret"), APP_ID("adobe-sign-connector.appId"),
	SECRET_KEY("adobe-sign-connector.secretKey"), USE_APP_PERMISSIONS("adobe-sign-connector.useAppPermissions"),
	CODE("adobe-sign-connector.code"), USE_USER_PASS_FLOW_ENABLED("adobe-sign-connector.useUserPassFlow.enabled"),
	USE_USER_PASS_FLOW_USER("adobe-sign-connector.useUserPassFlow.user"),
	USE_USER_PASS_FLOW_PASS("adobe-sign-connector.useUserPassFlow.pass");

	private String variableName;

	private AdobeVariable(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableName() {
		return variableName;
	}
}
