package com.nimbusds.login.rest.errors;

/**
 * Generic exception for indicating that the user's credentials are invalid.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class InvalidCredentialsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance of <code>InvalidCredentialsException</code> without detail message.
	 */
	public InvalidCredentialsException() {
	}

	/**
	 * Constructs an instance of <code>InvalidCredentialsException</code> with the specified detail message.
	 *
	 * @param msg the detail message.
	 */
	public InvalidCredentialsException(String msg) {
		super(msg);
	}
}
