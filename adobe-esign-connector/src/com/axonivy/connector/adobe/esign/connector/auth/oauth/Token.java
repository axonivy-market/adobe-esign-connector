package com.axonivy.connector.adobe.esign.connector.auth.oauth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.connector.adobe.esign.connector.util.Constants;

import ch.ivyteam.util.date.Now;

public class Token {
	private final Instant created;
	private final Map<String, Object> values;

	public Token(Map<String, Object> values) {
		this.values = values;
		this.created = Now.asInstant();
	}

	public Object value(String name) {
		return values.get(name);
	}

	public boolean hasAccessToken() {
		return StringUtils.isNotBlank(accessToken());
	}

	public String accessToken() {
		return (String) values.get(Constants.ACCESS_TOKEN);
	}

	public boolean hasRefreshToken() {
		return StringUtils.isNotBlank(refreshToken());
	}

	public String refreshToken() {
		return (String) values.get(Constants.REFRESH_TOKEN);
	}

	public boolean isExpired() {
		var expiresAt = created.plus(expiresIn(), ChronoUnit.SECONDS);
		return Instant.now().isAfter(expiresAt);
	}

	private int expiresIn() {
		var expiresIn = (Double) values.get(Constants.EXPIRES_IN);
		if (expiresIn == null) {
			return Integer.MAX_VALUE;
		}
		return expiresIn.intValue();
	}

	@Override
	public String toString() {
		return "Token [created=" + created + " expired=" + isExpired() + " values=" + values + "]";
	}
}