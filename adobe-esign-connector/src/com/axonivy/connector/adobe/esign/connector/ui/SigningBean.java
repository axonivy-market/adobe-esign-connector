package com.axonivy.connector.adobe.esign.connector.ui;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import api.rest.v6.client.AgreementDocuments;

@ManagedBean
@ViewScoped
public class SigningBean {

	private String agreementId;
	private String documentId;
	private AgreementDocuments documents;
	
	public void createAgreement() {
		
	}
	
	public String getAgreementId() {
		return agreementId;
	}
	
	public void setAgreementId(String agreementId) {
		this.agreementId = agreementId;
	}
	
	public String getDocumentId() {
		return documentId;
	}
	
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	
	public AgreementDocuments getDocuments() {
		return documents;
	}
	
	public void setDocuments(AgreementDocuments documents) {
		this.documents = documents;
	}
}
