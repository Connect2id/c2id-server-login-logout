package com.nimbusds.login.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.config.ApplicationProperties;
import com.nimbusds.login.domain.AuthenticationResult;
import com.nimbusds.login.domain.User;
import com.nimbusds.login.spi.UserAuthenticator;
import com.nimbusds.login.web.Utils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Connect2id authentication service for communicating with a Connect2id server.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
public class C2idAuthService {

	private final Logger logger = LoggerFactory.getLogger(C2idAuthService.class);
	private final ApplicationProperties config;
	private final UserAuthenticator authenticator;
	private static final ObjectMapper jsonMapper = new ObjectMapper();

	public C2idAuthService(ApplicationProperties config, UserAuthenticator authenticator) {
		this.config = config;
		this.authenticator = authenticator;
	}

	public ApplicationProperties getConfig() {
		return config;
	}

	public ObjectMapper getMapper() {
		return jsonMapper;
	}

	public UserAuthenticator getAuthenticator() {
		return authenticator;
	}

	/**
	 * @return the OpenID configuration of the server.
	 * @throws IOException If the web API request failed.
	 */
	public String getServerMetadata() throws IOException {
		return Request.Get(config.getEndpoint() + "/.well-known/openid-configuration")
				.connectTimeout(1000).socketTimeout(1000).execute().returnContent().asString();
	}

	/**
	 * Initialises the OpenID Connect login page. Submits the OpenID authentication request (encoded in the URI query
	 * string) and the session cookie (if any) to the Connect2id server. The Connect2id server will then process the
	 * request, and if necessary, ask the front-end to authenticate the subject (end-user), or obtain the subject's
	 * consent. When the OpenID authentication response is ready, the Connect2id server will ask the front-end to relay
	 * it back to the client (typically via a 302 HTTP redirect). For more information see
	 * http://connect2id.com/products/server/docs/integration/authz-session#authz-sessions-post
	 *
	 * @param queryString the initial query string from the client
	 * @param sessionId the "sid" from a cookie (if present)
	 * @return the response object from the OIDC server
	 * @throws IOException If the web API request failed.
	 */
	public Map<String, Object> initAuthRequest(String queryString, String sessionId) throws IOException {
		HashMap<String, String> body = new HashMap<>(2);
		body.put("query", queryString);
		if (!StringUtils.isBlank(sessionId)) {
			body.put("sub_sid", sessionId);
		}
		return Request.Post(config.getEndpoint() + config.getAuthz().getResourcePath()).
				addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAuthz().getAccessToken()).
				addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
				body(new ByteArrayEntity(jsonMapper.writeValueAsBytes(body))).
				execute().
				handleResponse((hr) -> handleC2idResponse(hr));
	}

	/**
	 * Authenticates the subject (end-user) by submitting the entered username and password to the LdapAuth JSON-RPC 2.0
	 * service (for LDAP authentication).
	 *
	 * @param user a {@link User} object holding the username and password
	 * @param authSessionId authz-session id (sid)
	 * @throws AuthenticationException If authentication failed.
	 * @return {@link User} object
	 */
	public Map<String, Object> authenticateSubject(User user, String authSessionId) throws IOException, AuthenticationException  {
		if (StringUtils.isBlank(authSessionId)) {
			logger.info("Bad 'authenticateSubject' request - empty SID.");
			return null;
		}
		AuthenticationResult result = authenticator.authenticate(user);
		Map<String, String> userDetails = new HashMap<>();
		Map<String, Object> subjectSessionClaims = Utils.extractSubjectSessionClaims(result);
		logger.debug("Authentication returned subject session claim names: {}", subjectSessionClaims.keySet());
		Map<String, Object> body = new HashMap<>();
		if (result != null && result.getSubject() != null) {
			userDetails.put("name", result.getSubject().getName());
			userDetails.put("email", result.getSubject().getEmail());
			body.put("sub", result.getSubject().getId());
			if (config.getAuthenticatorClass() != null) {
                // Optional Authentication Context Class Reference (ACR)
                body.put("acr", config.getAuthenticatorClass());
            }
			body.put("amr", new String[]{"pwd"}); // Optional Authentication Method References (AMR)
			body.put("data", userDetails);
		}
		return Request.Put(config.getEndpoint() + config.getAuthz().getResourcePath() + "/" + authSessionId).
				addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAuthz().getAccessToken()).
				addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
				body(new ByteArrayEntity(jsonMapper.writeValueAsBytes(body))).
				execute().
				handleResponse((hr) -> {
					Map<String, Object> response = handleC2idResponse(hr);
					String subjectSessionId = Utils.subjectSessionId(response);
					if (!subjectSessionClaims.isEmpty() && !StringUtils.isBlank(subjectSessionId)) {
						logger.debug("Updating subject session claims for session {}", subjectSessionId);
						try {
							Utils.updateSubjectSessionClaims(subjectSessionId, subjectSessionClaims, config, jsonMapper);
						} catch (IOException e) {
							logger.warn("Unable to update subject session claims: {}", e.getMessage());
						}
					} else if (!subjectSessionClaims.isEmpty()) {
						logger.warn("Authentication returned claims but no subject session SID was returned");
					}
					Utils.addClaimsToResponseSession(response, subjectSessionClaims);
					if (!response.isEmpty()) {
						response.put("data", userDetails);
					}
					return response;
				});
	}

	/**
	 * Handles the subject (end-user) authentication response from the LdapAuth JSON-RPC 2.0 service. On success subject
	 * (end-user) ID is submitted to the Connect2id server, along with a few other details to create a new subject
	 * (end-user) session.
	 *
	 * @param body request body
	 * @param authSessionId authz-session id (sid)
	 * @return authz response with auth type - 'auth' or 'consent' or 'response' or error
	 * @throws IOException If the web API request failed.
	 */
	public Map<String, Object> updateAuthRequest(String authSessionId, Map<String, Object> body) throws IOException {
		if (body == null || body.isEmpty() || StringUtils.isBlank(authSessionId)) {
			logger.info("Bad 'updateAuthRequest' request - empty body or SID.");
			return null;
		}
		logger.info("PUT authz-session with body {}", body);
		return Request.Put(config.getEndpoint() + config.getAuthz().getResourcePath() + "/" + authSessionId).
				addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAuthz().getAccessToken()).
				addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
				body(new ByteArrayEntity(jsonMapper.writeValueAsBytes(withOptionalProperties(body)))).
				execute().
				handleResponse((hr) -> handleC2idResponse(hr));
	}

	/**
	 * The subject (end-user) denies the authorisation request.
	 *
	 * @param authSessionId authz-session id (sid)
	 * @return authz response with auth type - 'auth' or 'consent' or 'response' or error
	 * @throws IOException If the web API request failed.
	 */
	public Map<String, Object> cancelAuthRequest(String authSessionId) throws IOException {
		if (StringUtils.isBlank(authSessionId)) {
			logger.info("Bad 'cancelAuthRequest' request - empty SID.");
			return null;
		}
		return Request.Delete(config.getEndpoint() + config.getAuthz().getResourcePath() + "/" + authSessionId).
				addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAuthz().getAccessToken()).
				addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
				execute().
				handleResponse((hr) -> handleC2idResponse(hr));
	}

	/**
	 * Retrieves an individual subject session. Used for SID verification.
	 * @param sessionId the subject SID
	 * @return a session object
	 * @throws IOException If the web API request failed.
	 */
	public Map<String, Object> verifySubject(String sessionId) throws IOException {
		return Request.Get(config.getEndpoint() + config.getSession().getResourcePath() + "/sessions").
				addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getSession().getAccessToken()).
				addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
				addHeader("SID", sessionId).
				execute().
				handleResponse((hr) -> {
					int status = hr.getStatusLine().getStatusCode();
					if (status == 404) {
						throw new IOException("Subject session not found (HTTP 404)");
					}
					if (status < 200 || status >= 300) {
						throw new IOException("Subject session lookup failed: " + hr.getStatusLine());
					}
					return handleC2idResponse(hr);
				});
	}

	/**
	 * Posts a new request to the logout session API.
	 * See https://connect2id.com/products/server/docs/integration/logout-session#logout-sessions-post
	 *
	 * @param queryString	The query string, {@code null} if none.
	 * @param sessionId		The subject SID, {@code null} if not found.
	 * @return				A logout prompt or logout error message.
	 * @throws IOException If the web API request failed.
	 */
	public Map<String, Object> logoutSession(String queryString, String sessionId) throws IOException {
		return logoutSession(queryString, sessionId, null);
	}

	/**
	 * Posts a new request to the logout session API, optionally marking it as
	 * initiated by the OpenID provider.
	 *
	 * @param queryString the original logout request parameters, or {@code null}
	 * @param sessionId the subject session ID, or {@code null}
	 * @param initiator {@code OP} for provider-initiated logout, or {@code null}
	 * @return a logout prompt, end or error message
	 * @throws IOException if the web API request failed
	 */
	public Map<String, Object> logoutSession(String queryString, String sessionId, String initiator)
			throws IOException {
		Map<String, Object> body = new HashMap<>(3);
		if (sessionId != null) {
			body.put("sub_sid", sessionId);
		}
		if (queryString != null) {
			body.put("query", queryString);
		}
		if (initiator != null) {
			body.put("initiator", initiator);
		}
		return Request.Post(config.getEndpoint() + config.getLogout().getResourcePath()).
				addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAuthz().getAccessToken()).
				addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
				body(new ByteArrayEntity(jsonMapper.writeValueAsBytes(body))).
				execute().
				handleResponse((hr) -> handleC2idResponse(hr));
	}

	/**
	 * Puts a logout confirmation to the logout session API.
	 *
	 * @param sessionId The subject SID, {@code null} if not found.
	 * @return the logout end message
	 * @throws IOException If the web API request failed.
	 */
	public Map<String, Object> logoutSessionConfirm(String logoutSessionId, boolean opLogout) throws IOException {
		Map<String, Boolean> body = new HashMap<>(2);
		body.put("op_logout", opLogout);
		// Keep compatibility with Connect2id server versions before 12.14.
		body.put("confirm_logout", opLogout);
		return Request.Put(config.getEndpoint() + config.getLogout().getResourcePath() + "/" + logoutSessionId).
				addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAuthz().getAccessToken()).
				addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
				body(new ByteArrayEntity(jsonMapper.writeValueAsBytes(body))).
				execute().
				handleResponse((hr) -> handleC2idResponse(hr));
	}

	private Map<String, Object> withOptionalProperties(Map<String, Object> body) {
		Map<String, Object> bodyMap = new HashMap<>();
		if (body.containsKey("scope") || body.containsKey("claims")) {
			bodyMap.put("scope", body.get("scope"));
			bodyMap.put("claims", body.get("claims"));
			bodyMap.put("preset_claims", new HashMap<String, Object>() {
				{
					put("id_token", Collections.emptyMap()); // Optional preset claims to return with the ID token
					put("userinfo", Collections.singletonMap("groups",
							Arrays.asList("admin", "audit"))); // Optional preset claims to return with UserInfo
				}
			});
			bodyMap.put("audience", Collections.emptyList()); // Optional custom audience values
			bodyMap.put("long_lived", config.getAuthz().isLongLived());
			bodyMap.put("id_token", new HashMap<String, Object>() {
				{
					put("lifetime", 600); // Override the default ID token lifetime (to 10 minutes)
//				put("impersonated_sub", "bob"); // optional subject to impersonate
				}
			});
			bodyMap.put("refresh_token", new HashMap<String, Object>() {
				{
					put("issue", true);
					put("lifetime", 0); // no expiration
				}
			});
			bodyMap.put("access_token", new HashMap<String, Object>() {
				{
					put("encoding", "SELF_CONTAINED"); // IDENTIFIER or SELF_CONTAINED
					put("lifetime", 600); // Override the default access token lifetime (to 10 minutes)
					put("encrypt", false); // Optional flag to encrypt the access token (for a self-contained one)
				}
			});
		}
		return bodyMap;
	}

	private Map<String, Object> handleC2idResponse(HttpResponse hr) throws IOException {
		HttpEntity entity = hr.getEntity();
		if (entity != null) {
			String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
			try {
				Map<String, Object> response = jsonMapper.readerFor(Map.class).readValue(body);
				logger.debug("Connect2id response: {}", response);
				return response;
			} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
				String contentType = hr.getFirstHeader(HttpHeaders.CONTENT_TYPE) != null
						? hr.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue() : "unknown";
				String preview = body.length() > 240 ? body.substring(0, 240) + "..." : body;
				throw new IOException("Connect2id returned a non-JSON response (HTTP "
						+ hr.getStatusLine().getStatusCode() + ", Content-Type " + contentType
						+ "): " + preview, e);
			}
		}
		logger.debug("Connect2id failed response: {}", hr.getStatusLine().getStatusCode());
		return Collections.emptyMap();
	}
}
