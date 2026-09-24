package com.nimbusds.login.auth;

/**
 * Indicates that the supplied user credentials are invalid.
 */
public class InvalidCredentialsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidCredentialsException() {
	}

	public InvalidCredentialsException(String message) {
		super(message);
	}
}
