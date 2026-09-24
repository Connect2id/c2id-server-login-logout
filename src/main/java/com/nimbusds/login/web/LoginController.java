package com.nimbusds.login.web;

import com.nimbusds.login.domain.User;
import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.auth.InvalidCredentialsException;
import com.nimbusds.login.service.C2idAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

	public LoginController(C2idAuthService authService, AuthzResponseDispatcher dispatcher) {
		this.authService = authService;
		this.dispatcher = dispatcher;
	}

	@GetMapping("/login")
	public String login(HttpServletRequest request, Model model) {
		model.addAttribute("actionUrl", currentUrl(request));
		model.addAttribute("cancelUrl", request.getContextPath() + "/login/cancel" + querySuffix(request));
		model.addAttribute("authenticationError", false);
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
		logger.debug("Making 'user.authGet' request to the LdapAuth service for user '{}'", user.getUsername());
		try {
			Map<String, Object> authzResponse = authService.authenticateSubject(user, authSessionId);
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
			@RequestParam(required = false) String state) {
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
		return "login";
	}

	private String currentUrl(HttpServletRequest request) {
		return request.getContextPath() + "/login" + querySuffix(request);
	}

	private String querySuffix(HttpServletRequest request) {
		String queryString = request.getQueryString();
		return StringUtils.isBlank(queryString) ? "" : "?" + queryString;
	}
}
