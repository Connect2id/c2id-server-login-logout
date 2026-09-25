package com.nimbusds.login.web;

import com.nimbusds.login.domain.User;
import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.auth.InvalidCredentialsException;
import com.nimbusds.login.service.C2idAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

	public LoginController(C2idAuthService authService, AuthzResponseDispatcher dispatcher,
			LocaleResolver localeResolver) {
		this.authService = authService;
		this.dispatcher = dispatcher;
		this.localeResolver = localeResolver;
	}

	@GetMapping("/login")
	@SuppressWarnings("unchecked")
	public String login(HttpServletRequest request, HttpServletResponse response, Model model) {
		Map<String, Object> authPrompt = authPrompt(request);
		applyRequestedLocale(authPrompt, request, response);
		model.addAttribute("actionUrl", currentUrl(request));
		model.addAttribute("cancelUrl", request.getContextPath() + "/login/cancel" + querySuffix(request));
		model.addAttribute("authenticationError", false);
		populatePrompt(model, authPrompt);
		return "login";
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
		String requiredSubject = requiredSubject(request);
		if (StringUtils.isNotBlank(requiredSubject) && !requiredSubject.equals(username)) {
			logger.info("Rejected authentication for a subject that does not match the requested subject");
			return loginError(model, request, user);
		}
		logger.debug("Making 'user.authGet' request to the LdapAuth service for user '{}'", user.getUsername());
		try {
			Map<String, Object> authzResponse = authService.authenticateSubject(user, authSessionId);
			request.getSession().removeAttribute("c2idAuthPrompt");
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
	 */
	@GetMapping("/login/cancel")
	public String cancel(@RequestParam(name = "redirect_uri", required = false) String redirectUri,
			@RequestParam(required = false) String state, HttpServletRequest request) {
		request.getSession().removeAttribute("c2idAuthPrompt");
		if (StringUtils.isBlank(redirectUri)) {
			return "redirect:/";
		}
		String delim = redirectUri.indexOf("?") < 0 ? "?" : "&";
		return "redirect:" + redirectUri + delim + "state=" + state + "&error=access_denied" +
				"&error_description=The user canceled the authentication.";
	}

	private String loginError(Model model, HttpServletRequest request, User user) {
		model.addAttribute("authenticationError", true);
		model.addAttribute("username", user.getUsername());
		model.addAttribute("actionUrl", currentUrl(request));
		model.addAttribute("cancelUrl", request.getContextPath() + "/login/cancel" + querySuffix(request));
		populatePrompt(model, authPrompt(request));
		return "login";
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> authPrompt(HttpServletRequest request) {
		Object value = request.getSession().getAttribute("c2idAuthPrompt");
		if (!(value instanceof Map)) {
			return null;
		}
		Map<String, Object> prompt = (Map<String, Object>) value;
		String authSessionId = request.getParameter("authSessionId");
		return authSessionId != null && authSessionId.equals(String.valueOf(prompt.get("sid"))) ? prompt : null;
	}

	private String requiredSubject(HttpServletRequest request) {
		Map<String, Object> prompt = authPrompt(request);
		return prompt == null ? null : value(prompt.get("required_sub"));
	}

	@SuppressWarnings("unchecked")
	private void populatePrompt(Model model, Map<String, Object> prompt) {
		if (prompt == null) {
			model.addAttribute("selectAccount", false);
			model.addAttribute("createAccount", false);
			return;
		}
		String requiredSubject = value(prompt.get("required_sub"));
		String loginHint = value(prompt.get("login_hint"));
		model.addAttribute("requiredSubject", requiredSubject);
		model.addAttribute("username", StringUtils.isNotBlank(requiredSubject) ? requiredSubject : loginHint);
		model.addAttribute("selectAccount", Boolean.TRUE.equals(prompt.get("select_account")));
		model.addAttribute("createAccount", Boolean.TRUE.equals(prompt.get("create_account")));
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

	private String value(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private String currentUrl(HttpServletRequest request) {
		return request.getContextPath() + "/login" + querySuffix(request);
	}

	private String querySuffix(HttpServletRequest request) {
		String queryString = request.getQueryString();
		return StringUtils.isBlank(queryString) ? "" : "?" + queryString;
	}
}
