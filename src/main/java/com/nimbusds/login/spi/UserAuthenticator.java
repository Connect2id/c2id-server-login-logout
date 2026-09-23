package com.nimbusds.login.spi;

import com.nimbusds.login.rest.errors.AuthenticationException;
import com.nimbusds.login.domain.AuthenticationResult;
import com.nimbusds.login.domain.User;

/**
 * Authenticates a subject with an external service.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public interface UserAuthenticator {

	public AuthenticationResult authenticate(User user) throws AuthenticationException;

}
