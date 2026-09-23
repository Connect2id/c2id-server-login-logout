package com.nimbusds.login.web;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes and decodes the error message that is shown on the error page, replacing the client-side
 * Base64 utility of the previous AngularJS front-end.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class ErrorMessages {

	private ErrorMessages() {
	}

	/**
	 * @param message the error message, may be null
	 * @return the URL-safe Base64 encoded message, safe to place in an URL path segment
	 */
	public static String encode(String message) {
		if (message == null) {
			message = "Authentication failed.";
		}
		return Base64.getUrlEncoder().withoutPadding().
				encodeToString(message.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @param encoded the URL-safe Base64 encoded message, may be null
	 * @return the decoded message, or the input itself if it cannot be decoded
	 */
	public static String decode(String encoded) {
		if (encoded == null || encoded.isBlank()) {
			return null;
		}
		try {
			return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			return encoded;
		}
	}
}
