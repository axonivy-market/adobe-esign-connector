package com.axonivy.connector.adobe.esign.connector.auth.oauth;

import com.axonivy.connector.adobe.esign.connector.auth.JacksonUtils;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Class for storing OAuth token to Variable
 * @author stefan.masek
 *
 */
public class VarTokenStore {
	private final String varName;

	public static VarTokenStore get(String varName) {
		return new VarTokenStore(varName);
	}

	VarTokenStore(String varName) {
		this.varName = varName;
	}

	/**
	 * Loads Variable and tries to create the token object {@link Token}
	 * @return
	 */
	public Token getToken() {
		String tokenVar = Ivy.var().get(varName);
		return JacksonUtils.convertJsonToObject(tokenVar, Token.class);
	}

	/**
	 * Stores token object {@link Token} as a json to Variable
	 * @param token
	 */
	public void setToken(Token token) {
		String tokenString = JacksonUtils.writeObjectAsJson(token);
		Ivy.var().set(varName, tokenString);
	}
}
