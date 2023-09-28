package com.axonivy.connector.adobe.esign.connector.enums;

import ch.ivyteam.ivy.environment.Ivy;

public enum AdobeVariable {
	BASE_URI("adobe-sign-connector.baseUri"), 
	HOST("adobe-sign-connector.host"),
	INTEGRATION_KEY("adobe-sign-connector.integrationKey"), 
	RETURN_PAGE("adobe-sign-connector.returnPage"),
	PERMISSIONS("adobe-sign-connector.permissions"), 
	CLIENT_ID("adobe-sign-connector.clientId"),
	CLIENT_SECRET("adobe-sign-connector.clientSecret"), 
	APP_ID("adobe-sign-connector.appId"),
	SECRET_KEY("adobe-sign-connector.secretKey"), 
	USE_APP_PERMISSIONS("adobe-sign-connector.useAppPermissions"),
	CODE("adobe-sign-connector.code"), 
	USE_USER_PASS_FLOW_ENABLED("adobe-sign-connector.useUserPassFlow.enabled"),
	USE_USER_PASS_FLOW_USER("adobe-sign-connector.useUserPassFlow.user"),
	USE_USER_PASS_FLOW_PASS("adobe-sign-connector.useUserPassFlow.pass"),
	OAUTH_TOKEN("adobe-sign-connector.oauthToken"),
	ACCESS_TOKEN("adobe-sign-connector.accessToken"),
	AUTHENTICATION_URI("adobe-sign-connector.authenticationUri");

	private String variableName;

	private AdobeVariable(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableName() {
		return variableName;
	}
	
	public String getValue() {
		return Ivy.var().get(variableName);
	}
	
	/**
	 * Sets new value to the Variable
	 * @param newValue
	 * @return old value that was set before. Empty string if it was not defined.
	 */
	public String updateValue(String newValue) {
		return Ivy.var().set(variableName, newValue);
	}
}
