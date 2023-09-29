package com.axonivy.connector.adobe.esign.connector.rest;

import ch.ivyteam.ivy.bpm.error.BpmError;

public class UploadResult {
	
	String id;
	BpmError error;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public BpmError getError() {
		return error;
	}
	
	public void setError(BpmError error) {
		this.error = error;
	}
	
}
