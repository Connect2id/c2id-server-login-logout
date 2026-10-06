package com.nimbusds.login.web;

import com.nimbusds.login.rest.utils.SessionCookieManager;
import com.nimbusds.login.service.C2idAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Consent page controller. Lets the subject (end-user) grant or deny the requested OpenID Connect
 * scopes and claims, and lets the subject log out and start over.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class ConsentController {

	private static final List<String> SCOPE_ORDER = List.of(
			"openid", "profile", "email", "address", "phone", "offline_access");
	private static final List<String> CLAIM_ORDER = List.of(
			"sub", "name", "family_name", "given_name", "middle_name", "nickname",
			"preferred_username", "profile", "picture", "website", "gender", "birthdate",
			"zoneinfo", "locale", "updated_at", "email", "email_verified", "address",
			"phone_number", "phone_number_verified");

	private final Logger logger = LoggerFactory.getLogger(ConsentController.class);
	private final C2idAuthService authService;
	private final AuthzResponseDispatcher dispatcher;
	private final SessionCookieManager sessionCookieManager;

	public ConsentController(C2idAuthService authService, AuthzResponseDispatcher dispatcher,
			SessionCookieManager sessionCookieManager) {
		this.authService = authService;
		this.dispatcher = dispatcher;
		this.sessionCookieManager = sessionCookieManager;
	}

	@GetMapping("/consent")
	public String consent(HttpServletRequest request, Model model,
			@RequestParam(required = false) String authSessionId) {
		Map<String, Object> authzResponse = getAuthzResponse(request);
		if (authzResponse == null) {
			return "redirect:/err/" + ErrorMessages.encode("Authentication failed.");
		}
		model.addAttribute("authSessionId", authSessionId);
		addConsentModel(model, authzResponse);
		return "consent";
	}

	@PostMapping("/consent")
	public String submitConsent(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam String authSessionId,
			@RequestParam(name = "scope", required = false) List<String> scopes,
			@RequestParam(name = "claim", required = false) List<String> claims) throws IOException {
		// The "openid" scope is always granted
		List<String> scopeList = new ArrayList<>(scopes != null ? scopes : Collections.emptyList());
		if (!scopeList.contains("openid")) {
			scopeList.add(0, "openid");
		}
		Map<String, Object> body = new HashMap<>(2);
		body.put("scope", scopeList);
		body.put("claims", claims != null ? claims : Collections.emptyList());
		logger.debug("Submitting subject authentication (sid={}) to Connect2id server: {}", authSessionId, body);
		Map<String, Object> authzResponse = authService.updateAuthRequest(authSessionId, body);
		return dispatcher.dispatch(authzResponse, request, response, model);
	}

	/**
	 * The subject (end-user) denies the authorisation request.
	 */
	@PostMapping("/consent/deny")
	public String denyAuthorization(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam String authSessionId) throws IOException {
		logger.debug("Authorization denied, submitting request to Connect2id server...");
		Map<String, Object> authzResponse = authService.cancelAuthRequest(authSessionId);
		return dispatcher.dispatch(authzResponse, request, response, model);
	}

	/**
	 * The subject (end-user) logs out and the login flow starts over with the original query string.
	 */
	@PostMapping("/consent/logout")
	public String logoutSubject(HttpServletRequest request, HttpServletResponse response, Model model)
			throws IOException {
		logger.debug("Submitting subject logout request to Connect2id server.");
		String sessionId = sessionCookieManager.getCurrentSessionId(request, authService.getConfig().getCookieName());
		authService.logoutSessionConfirm(sessionId, false);
		sessionCookieManager.removeSession(request, response, authService.getConfig().getCookieName(), sessionId);
		// The session id cookie was removed, so the flow starts with a blank session
		String queryString = (String) request.getSession().getAttribute("c2idQueryString");
		Map<String, Object> authzResponse = authService.initAuthRequest(queryString, null);
		return dispatcher.dispatch(authzResponse, request, response, model);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getAuthzResponse(HttpServletRequest request) {
		return (Map<String, Object>) request.getSession().getAttribute("c2idAuthzResponse");
	}

	/**
	 * Flattens the Connect2id consent response into the model used by the consent template.
	 */
	private void addConsentModel(Model model, Map<String, Object> authzResponse) {
		Map<String, Object> subSession = nestedMap(authzResponse, "sub_session");
		model.addAttribute("authenticatedUser", mergeSessionAttributes(subSession));
		Map<String, Object> client = nestedMap(authzResponse, "client");
		if (client != null) {
			model.addAttribute("clientName", client.get("name"));
			model.addAttribute("clientId", client.get("client_id"));
		}
		model.addAttribute("newScopes", ordered(nestedList(authzResponse, "scope", "new"), SCOPE_ORDER));
		model.addAttribute("consentedScopes", ordered(nestedList(authzResponse, "scope", "consented"), SCOPE_ORDER));
		model.addAttribute("newEssentialClaims",
				ordered(nestedList(authzResponse, "claims", "new", "essential"), CLAIM_ORDER));
		model.addAttribute("newVoluntaryClaims",
				ordered(nestedList(authzResponse, "claims", "new", "voluntary"), CLAIM_ORDER));
		model.addAttribute("consentedEssentialClaims",
				ordered(nestedList(authzResponse, "claims", "consented", "essential"), CLAIM_ORDER));
		model.addAttribute("consentedVoluntaryClaims",
				ordered(nestedList(authzResponse, "claims", "consented", "voluntary"), CLAIM_ORDER));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> mergeSessionAttributes(Map<String, Object> subSession) {
		if (subSession == null) {
			return null;
		}
		Map<String, Object> user = new HashMap<>();
		if (subSession.get("data") instanceof Map) {
			user.putAll((Map<String, Object>) subSession.get("data"));
		}
		if (subSession.get("claims") instanceof Map) {
			user.putAll((Map<String, Object>) subSession.get("claims"));
		}
		return user.isEmpty() ? null : user;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> nestedMap(Map<String, Object> source, String... path) {
		Map<String, Object> current = source;
		for (String key : path) {
			if (current == null) {
				return null;
			}
			current = (Map<String, Object>) current.get(key);
		}
		return current;
	}

	@SuppressWarnings("unchecked")
	private List<String> nestedList(Map<String, Object> source, String... path) {
		Map<String, Object> parent = nestedMap(source, path.length > 1
				? java.util.Arrays.copyOf(path, path.length - 1) : new String[0]);
		if (parent == null) {
			return Collections.emptyList();
		}
		Object value = parent.get(path[path.length - 1]);
		if (value instanceof List) {
			return (List<String>) value;
		}
		return Collections.emptyList();
	}

	private List<String> ordered(List<String> values, List<String> priority) {
		return values.stream()
				.sorted(Comparator.comparingInt((String value) -> {
					int index = priority.indexOf(value);
					return index >= 0 ? index : priority.size();
				}).thenComparing(String::compareTo))
				.toList();
	}
}
