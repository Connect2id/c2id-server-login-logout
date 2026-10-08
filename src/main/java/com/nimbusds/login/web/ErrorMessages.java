package com.nimbusds.login.web;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes and decodes the error message that is shown on the error page.
 *
 * <p>The {@code encode} method Base64-encodes a text message for safe placement in a URL path segment.
 * The {@code decode} method decodes a URL-safe Base64 encoded message back to its original text.</p>
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class ErrorMessages {

	private ErrorMessages() {
	}

	/**
	 * Encodes a text message to URL-safe Base64 for safe placement in a URL path segment.
	 *
	 * @param message the error message, may be null; if null, "Authentication failed." is used
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
	 * Decodes a URL-safe Base64 encoded message back to its original text.
	 *
	 * @param encoded the URL-safe Base64 encoded message, may be null; if null or blank, returns null
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
