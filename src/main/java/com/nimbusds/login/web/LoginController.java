package com.nimbusds.login.web;

import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.auth.InvalidCredentialsException;
import com.nimbusds.login.domain.User;
import com.nimbusds.login.rest.utils.SessionCookieManager;
import com.nimbusds.login.service.C2idAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Login page controller. Submits the entered username and password to the configured
 * {@link com.nimbusds.login.spi.UserAuthenticator} and then handles the Connect2id response.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class LoginController {

	private final Logger logger = LoggerFactory.getLogger(LoginController.class);
	private final C2idAuthService authService;
	private final AuthzResponseDispatcher dispatcher;
	private final LocaleResolver localeResolver;
	private final SessionCookieManager sessionCookieManager;

	public LoginController(C2idAuthService authService, AuthzResponseDispatcher dispatcher,
			LocaleResolver localeResolver, SessionCookieManager sessionCookieManager) {
		this.authService = authService;
		this.dispatcher = dispatcher;
		this.localeResolver = localeResolver;
		this.sessionCookieManager = sessionCookieManager;
	}

	@GetMapping("/login")
	@SuppressWarnings("unchecked")
	public String login(HttpServletRequest request, HttpServletResponse response, Model model) {
		Map<String, Object> authPrompt = Utils.authPrompt(request);
		applyRequestedLocale(authPrompt, request, response);
		model.addAttribute("actionUrl", Utils.currentUrl(request));
		model.addAttribute("cancelUrl", request.getContextPath() + "/login/cancel" + Utils.querySuffix(request));
		model.addAttribute("authSessionId", request.getParameter("authSessionId"));
		model.addAttribute("authenticationError", false);
		boolean newAccountMode = Boolean.TRUE.equals(request.getSession().getAttribute("newAccountLogin"));
		Utils.populatePrompt(model, authPrompt, newAccountMode);
		if (authPrompt != null && !newAccountMode) {
			List<Map<String, Object>> accounts = availableAccounts(request, response);
			model.addAttribute("availableAccounts", accounts);
			model.addAttribute("accountSelectionError",
					Utils.booleanValue(authPrompt.get("select_account")) && accounts.isEmpty());
		}
		return "login";
	}

	@PostMapping("/login/select")
	public String selectAccount(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam String authSessionId, @RequestParam String slot) throws IOException {
		Map<String, Object> prompt = Utils.authPrompt(request);
		logger.debug("Selecting account slot '{}' for authorization session {}", slot, authSessionId);
		if (prompt == null || !Utils.booleanValue(prompt.get("select_account"))) {
			logger.warn("Account selection rejected because the select-account prompt is missing or inactive");
			return Utils.selectionError(request, model, authSessionId);
		}
		String sessionId = sessionCookieManager.getSessions(request, authService.getConfig().getCookieName()).get(slot);
		if (StringUtils.isBlank(sessionId)) {
			logger.warn("Account selection rejected because slot '{}' is not present", slot);
			return Utils.selectionError(request, model, authSessionId);
		}
		try {
			authService.verifySubject(sessionId);
		} catch (IOException e) {
			logger.warn("Selected account session '{}' is no longer available: {}", slot, e.getMessage());
			sessionCookieManager.removeSession(request, response, authService.getConfig().getCookieName(), sessionId);
			return Utils.selectionError(request, model, authSessionId);
		}
		String query = Utils.removeSelectAccountPrompt((String) request.getSession().getAttribute("c2idQueryString"));
		request.getSession().setAttribute("c2idQueryString", query);
		request.getSession().setAttribute("newAccountLogin", false);
		try {
			authService.cancelAuthRequest(authSessionId);
		} catch (IOException e) {
			logger.debug("The previous authorization session could not be cancelled: {}", e.getMessage());
		}
		sessionCookieManager.activateSession(request, response, authService.getConfig().getCookieName(), sessionId);
		Map<String, Object> authzResponse;
		try {
			authzResponse = authService.initAuthRequest(query, sessionId);
		} catch (IOException e) {
			logger.error("Could not restart authorization for selected session {}: {}", slot, e.getMessage());
			return Utils.selectionError(request, model, authSessionId);
		}
		logger.debug("Authorization restart for slot '{}' returned type '{}'", slot,
				authzResponse != null ? authzResponse.get("type") : null);
		request.getSession().removeAttribute("c2idAuthPrompt");
		return dispatcher.dispatch(authzResponse, request, response, model);
	}

	@PostMapping("/login/new-account")
	public String newAccount(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam String authSessionId) throws IOException {
		Map<String, Object> prompt = Utils.authPrompt(request);
		if (prompt == null || !Utils.booleanValue(prompt.get("select_account"))) {
			return Utils.selectionError(request, model, authSessionId);
		}
		String query = Utils.removeSelectAccountPrompt(
				(String) request.getSession().getAttribute("c2idQueryString"));
		request.getSession().setAttribute("c2idQueryString", query);
		try {
			authService.cancelAuthRequest(authSessionId);
		} catch (IOException e) {
			logger.debug("The previous authorization session could not be cancelled: {}", e.getMessage());
		}
		request.getSession().removeAttribute("c2idAuthPrompt");
		Map<String, Object> authzResponse = authService.initAuthRequest(query, null);
		return dispatcher.dispatch(authzResponse, request, response, model);
	}

	@PostMapping("/login")
	public String loginSubmit(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam(required = false) String username,
			@RequestParam(required = false) String password,
			@RequestParam(required = false) String authSessionId) throws IOException {
		if (StringUtils.isBlank(authSessionId)) {
			return "redirect:/err/" + ErrorMessages.
					encode("The authorisation session identifier is missing.");
		}
		User user = new User();
		user.setUsername(username);
		user.setPassword(password);
		if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
			return loginError(model, request, user);
		}
		String requiredSubject = Utils.requiredSubject(request);
		if (StringUtils.isNotBlank(requiredSubject) && !requiredSubject.equals(username)) {
			logger.info("Rejected authentication for a subject that does not match the requested subject");
			return loginError(model, request, user);
		}
		logger.debug("Making 'user.authGet' request to the LdapAuth service for user '{}'", user.getUsername());
		try {
			Map<String, Object> authzResponse = authService.authenticateSubject(user, authSessionId);
			request.getSession().removeAttribute("c2idAuthPrompt");
			request.getSession().removeAttribute("newAccountLogin");
			return dispatcher.dispatch(authzResponse, request, response, model);
		} catch (AuthenticationException e) {
			logger.info("Authentication failed for user '{}': {}", user.getUsername(), e.getMessage());
			return loginError(model, request, user);
		} catch (InvalidCredentialsException e) {
			logger.info("Invalid credentials for user '{}': {}", user.getUsername(), e.getMessage());
			return loginError(model, request, user);
		}
	}

	/**
	 * The subject (end-user) cancels the login and is returned to the OpenID client with
	 * an "access_denied" error.
	 *
	 * <p>The cancellation is submitted to the Connect2id server, which validates the client
	 * redirection URI against the client registration and builds the error response. The
	 * "redirect_uri" request parameter is never used as a redirect target on its own, so this
	 * endpoint cannot be turned into an open redirect on the OpenID provider origin.
	 */
	@GetMapping("/login/cancel")
	public String cancel(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam(name = "authSessionId", required = false) String authSessionId) {
		request.getSession().removeAttribute("c2idAuthPrompt");
		request.getSession().removeAttribute("newAccountLogin");
		if (StringUtils.isBlank(authSessionId)) {
			return "redirect:/";
		}
		try {
			Map<String, Object> authzResponse = authService.cancelAuthRequest(authSessionId);
			return dispatcher.dispatch(authzResponse, request, response, model);
		} catch (IOException e) {
			logger.error("Failed to cancel the authentication request: {}", e.getMessage());
			return "redirect:/";
		}
	}

	private String loginError(Model model, HttpServletRequest request, User user) {
		model.addAttribute("authenticationError", true);
		model.addAttribute("username", user.getUsername());
		model.addAttribute("actionUrl", Utils.currentUrl(request));
		model.addAttribute("cancelUrl", request.getContextPath() + "/login/cancel" + Utils.querySuffix(request));
		model.addAttribute("authSessionId", request.getParameter("authSessionId"));
		Utils.populatePrompt(model, Utils.authPrompt(request),
				Boolean.TRUE.equals(request.getSession().getAttribute("newAccountLogin")));
		return "login";
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> availableAccounts(HttpServletRequest request, HttpServletResponse response) {
		List<Map<String, Object>> accounts = new ArrayList<>();
		String currentSessionId = sessionCookieManager.getCurrentSessionId(request,
				authService.getConfig().getCookieName());
		Map<String, String> sessionCookies = sessionCookieManager
				.getSessions(request, authService.getConfig().getCookieName());
		logger.debug("Account selection found session slots: {}", sessionCookies.keySet());
		for (Map.Entry<String, String> entry : sessionCookies.entrySet()) {
			try {
				Map<String, Object> session = authService.verifySubject(entry.getValue());
				if (session.containsKey("error") || session.containsKey("error_description")) {
					logger.warn("Session lookup for slot '{}' returned an API error: {}", entry.getKey(), session);
					continue;
				}
				Map<String, Object> profile = new HashMap<>();
				if (session.get("data") instanceof Map) {
					profile.putAll((Map<String, Object>) session.get("data"));
				}
				if (session.get("claims") instanceof Map) {
					profile.putAll((Map<String, Object>) session.get("claims"));
				}
				if (profile.isEmpty() && session.get("sub") != null) {
					profile.put("name", session.get("sub"));
				}
				if (profile.isEmpty()) {
					logger.warn("Session lookup for slot '{}' returned no profile data", entry.getKey());
					continue;
				}
				profile.put("slot", entry.getKey());
				profile.put("current", entry.getValue().equals(currentSessionId));
				accounts.add(profile);
			} catch (IOException e) {
				logger.warn("Unable to retrieve account session {}: {}", entry.getKey(), e.getMessage());
				if (e.getMessage() != null && e.getMessage().contains("HTTP 404")) {
					sessionCookieManager.removeSession(request, response, authService.getConfig().getCookieName(), entry.getValue());
				}
			}
		}
		return accounts;
	}


	@SuppressWarnings("unchecked")
	private void applyRequestedLocale(Map<String, Object> prompt, HttpServletRequest request,
			HttpServletResponse response) {
		if (prompt == null || !(prompt.get("ui_locales") instanceof List)) {
			return;
		}
		for (Object value : (List<Object>) prompt.get("ui_locales")) {
			if (value != null) {
				Locale locale = Locale.forLanguageTag(value.toString());
				if (!locale.toLanguageTag().isBlank()) {
					localeResolver.setLocale(request, response, locale);
					return;
				}
			}
		}
	}

}
