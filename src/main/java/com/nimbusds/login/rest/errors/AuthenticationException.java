package com.nimbusds.login.rest.errors;

/**
 * Generic exception for indicating an authentication failure.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class AuthenticationException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public AuthenticationException() {
		super("Authentication failed.");
	}

	/**
	 * Constructs a new instance with the specified detail message.
	 * @param msg the detail message.
	 */
	public AuthenticationException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a new instance with the specified detail message and throwable.
	 * @param message the detail message
	 * @param cause cause
	 */
	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

}
