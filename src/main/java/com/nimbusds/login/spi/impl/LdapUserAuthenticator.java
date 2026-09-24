package com.nimbusds.login.spi.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.login.config.ApplicationProperties;
import com.nimbusds.login.domain.AuthenticationResult;
import com.nimbusds.login.domain.User;
import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.auth.InvalidCredentialsException;
import com.nimbusds.login.spi.UserAuthenticator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example authenticator using Connect2id LdapAuth.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class LdapUserAuthenticator implements UserAuthenticator {

	private final Logger logger = LoggerFactory.getLogger(UserAuthenticator.class);
	private final ApplicationProperties config;
	private static final ObjectMapper jsonMapper = new ObjectMapper();

	public LdapUserAuthenticator(ApplicationProperties config) {
		this.config = config;
	}

	@Override
	@SuppressWarnings("unchecked")
	public AuthenticationResult authenticate(User user) throws AuthenticationException {
		if (user == null || StringUtils.isBlank(user.getPassword()) || StringUtils.isBlank(user.getUsername())) {
			logger.info("Username or password should not be empty. {}", user);
			throw new AuthenticationException("LdapAuth failed, invalid parameters given.");
		}
		HashMap<String, Object> body = new HashMap<>();
		body.put("id", 1);
		body.put("jsonrpc", "2.0");
		body.put("method", "user.authGet");
		body.put("params", new HashMap<String, String>() {
			{
				put("username", user.getUsername());
				put("password", user.getPassword());
				put("apiKey", config.getLdapAuth().getApiKey());
			}
		});
		// Make a user.auth request to the LdapAuth JSON-RPC 2.0 service
		try {
			return Request.Post(config.getLdapAuth().getEndpoint()).
					addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
					body(new ByteArrayEntity(jsonMapper.writeValueAsBytes(body))).
					execute().
					handleResponse((hr) -> {
						HttpEntity entity = hr.getEntity();
						if (entity != null) {
							JsonNode response = jsonMapper.readTree(entity.getContent());
							logger.debug("LdapAuth response: {}", response);
							if (!response.hasNonNull("error")) {
								user.setId(response.path("result").path("attributes").path("userID").asText());
								user.setName(response.path("result").path("attributes").path("name").asText());
								user.setEmail(response.path("result").path("attributes").path("email").path(0).asText());
								logger.debug("Subject successfully authenticated: {}", user);
								Map<String, Object> details = jsonMapper.readValue(response.traverse(), Map.class);
								return new AuthenticationResult(user, details);
							}
						}
						logger.info("LdapAuth response status: {}", hr.getStatusLine());
						throw new InvalidCredentialsException("LdapAuth failed for user " + user.toString());
					});
		} catch (IOException e) {
			throw new AuthenticationException("LdapAuth failed for user " + user.toString(), e);
		}
	}

}
