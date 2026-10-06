/*
 * Copyright 2013-2026 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */
package com.nimbusds.login.spi.impl;

import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.config.ApplicationProperties;
import com.nimbusds.login.domain.User;
import com.nimbusds.login.spi.FederatedSubjectResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * This is for local testing only. Do not use in production!
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class ExampleSubjectResolver implements FederatedSubjectResolver {

	private final ApplicationProperties config;

	public ExampleSubjectResolver(ApplicationProperties config) {
		this.config = config;
	}

	/**
	 * This is just an example of a custom mapping between the third-party IdP and Connect2id.
	 * In a real scenario, this method would look up the user by {@code sub} or any other attribute,
	 * and return that user before continuing the authorization flow.
	 * @param authentication a Spring Security Authentication object
	 * @return a Connect2id user
	 * @throws AuthenticationException thrown if authentication is of the wrong type, or no matching user was found.
	 */
	@Override
	public User resolve(Authentication authentication) throws AuthenticationException {
		if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
			throw new AuthenticationException("Unsupported federated authentication.");
		}
		if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
			String provider = oauthToken.getAuthorizedClientRegistrationId();
			oidcUser.getSubject();
			User u = new User();
			u.setId(provider + "_" + oidcUser.getSubject());
			u.setName(oidcUser.getFullName());
			u.setUsername(oidcUser.getEmail());
			u.setEmail(oidcUser.getEmail());
			return u;
		} else if (config.isDemoMode()) {
			User u = new User();
			u.setId("alice");
			u.setName("Alice [federated]");
			u.setEmail("alice@wonderland.org");
			u.setUsername("alice");
			return u;
		}
		throw new AuthenticationException("User not found.");
	}

}
