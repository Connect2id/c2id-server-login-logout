package com.nimbusds.login.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Holds the authenticated subject (User) and additional information.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class AuthenticationResult {

	private User subject;
	private Map<String, Object> details;

	public AuthenticationResult() {
	}

	public AuthenticationResult(User subject, Map<String, Object> details) {
		this.subject = subject;
		this.details = details;
	}

	/**
	 * Get the value of subject
	 *
	 * @return the value of subject
	 */
	public User getSubject() {
		return subject;
	}

	/**
	 * Set the value of subject
	 *
	 * @param subject new value of subject
	 */
	public void setSubject(User subject) {
		this.subject = subject;
	}

	/**
	 * Get the value of details
	 *
	 * @return the value of details
	 */
	public Map<String, Object> getDetails() {
		if (details == null) {
			return Collections.emptyMap();
		}
		return details;
	}

	/**
	 * Set the value of details
	 *
	 * @param details new value of details
	 */
	public void setDetails(Map<String, Object> details) {
		this.details = details;
	}


}
