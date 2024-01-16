package com.axonivy.connector.adobe.esign.connector.rest;

public class UploadWrapper {

	private byte[] bytes;
	private String filename;

	public UploadWrapper(String filename, byte[] bytes) {
		this.filename = filename;
		this.bytes = bytes;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}
