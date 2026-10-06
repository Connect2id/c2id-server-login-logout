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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.config.ApplicationProperties;
import com.nimbusds.login.domain.User;
import com.nimbusds.login.spi.FederatedSubjectResolver;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ByteArrayEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * This is another example implementation of the subject resolver (mapper).
 * This one relies on the LdapAuth connector for looking up the users in Connect2id.
 *
 * <b>This class requires that you
 * <a href="https://connect2id.com/products/ldapauth/config#ldapAuth-api-allowUserGetRequests">
 *	enable the "user.get" JSON-RPC method.
 * </a>
 * </b>
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class LdapSubjectResolver implements FederatedSubjectResolver {

	private final ApplicationProperties config;
	private final JsonMapper jsonMapper = new JsonMapper();

	public LdapSubjectResolver(ApplicationProperties config) {
		this.config = config;
	}

	@Override
	public User resolve(Authentication authentication) throws AuthenticationException {
		if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
			throw new AuthenticationException("Unsupported federated authentication.");
		}
		String provider = oauthToken.getAuthorizedClientRegistrationId();
		String upstreamSubject;
		if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
			upstreamSubject = oidcUser.getSubject();
		} else {
			upstreamSubject = oauthToken.getName();
		}
		if (StringUtils.isBlank(provider) || StringUtils.isBlank(upstreamSubject)) {
			throw new AuthenticationException("Federated authentication has no subject.");
		}
		return lookup(subjectPrefix(provider, upstreamSubject));
	}

	private User lookup(String uid) throws AuthenticationException {
		try {
			Map<String, Object> params = new HashMap<>();
			params.put("username", uid);
			params.put("apiKey", config.getLdapAuth().getApiKey());
			Map<String, Object> request = new HashMap<>();
			request.put("method", "user.get");
			request.put("params", params);
			request.put("id", "federated-subject");
			request.put("jsonrpc", "2.0");
			JsonNode response = Request.Post(config.getLdapAuth().getEndpoint())
					.addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
					.body(new ByteArrayEntity(jsonMapper.writeValueAsBytes(request)))
					.execute()
					.handleResponse(hr -> {
						if (hr.getEntity() == null) {
							return jsonMapper.createObjectNode();
						}
						return jsonMapper.readTree(hr.getEntity().getContent());
					});
			if (response.has("error")) {
				throw new AuthenticationException("Federated subject lookup failed.");
			}
			JsonNode result = response.get("result");
			if (result == null || result.isNull()) {
				if (config.isDemoMode()) {
					// for demo and testing purposes only
					User u = new User();
					u.setId("alice");
					u.setName("Alice [federated]");
					u.setEmail("alice@wonderland.org");
					u.setUsername("alice");
					return u;
				} else {
					throw new AuthenticationException("No local account is mapped to federated subject.");
				}
			}
			JsonNode attributes = result.path("attributes");
			User user = new User();
			user.setId(text(attributes, "userID", uid));
			user.setUsername(text(attributes, "uid", uid));
			user.setName(text(attributes, "name", user.getUsername()));
			String email = text(attributes, "email", null);
			if (email != null) {
				user.setEmail(email);
			}
			return user;
		} catch (IOException e) {
			throw new AuthenticationException("Federated subject lookup failed.", e);
		}
	}

	private String text(JsonNode object, String name, String fallback) {
		JsonNode value = object.get(name);
		if (value == null || value.isNull()) {
			return fallback;
		}
		return value.isArray() && !value.isEmpty() ? value.get(0).asText() : value.asText(fallback);
	}

}
