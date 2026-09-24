package com.nimbusds.login.auth;

/**
 * Indicates a failure while authenticating a user.
 */
public class AuthenticationException extends Exception {

	private static final long serialVersionUID = 1L;

	public AuthenticationException() {
		super("Authentication failed.");
	}

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}
}
