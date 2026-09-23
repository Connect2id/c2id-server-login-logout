package com.nimbusds.login.web;

import com.nimbusds.login.rest.utils.CookieManager;
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

/**
 * Home page controller. Entry point of every OpenID Connect authentication request. If the request
 * contains a "client_id" parameter, the whole query string is submitted to the Connect2id server to
 * initialise the authorisation session. Otherwise the logout screen is shown, if the subject has an
 * active session.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class HomeController {

	private final Logger logger = LoggerFactory.getLogger(HomeController.class);
	private final C2idAuthService authService;
	private final AuthzResponseDispatcher dispatcher;

	public HomeController(C2idAuthService authService, AuthzResponseDispatcher dispatcher) {
		this.authService = authService;
		this.dispatcher = dispatcher;
	}

	@GetMapping("/")
	public String home(HttpServletRequest request, HttpServletResponse response,
			Model model) throws IOException {
		String queryString = request.getQueryString();
		if (StringUtils.isNotBlank(request.getParameter("client_id"))) {
			// Remember the original query string for the login, consent and logout flows
			request.getSession().setAttribute("c2idQueryString", queryString);
			String sessionId = CookieManager.getCookie(request, authService.getConfig().getCookieName());
			logger.debug("Initial authorization request to Connect2id server: sid={}, qs={}", sessionId, queryString);
			Map<String, Object> authzResponse = authService.initAuthRequest(queryString, sessionId);
			return dispatcher.dispatch(authzResponse, request, response, model);
		}
		model.addAttribute("authenticatedUser", getAuthenticatedUser(request));
		return "home";
	}

	@PostMapping("/logout")
	public String logout(HttpServletRequest request, Model model) {
		String sessionId = CookieManager.getCookie(request, authService.getConfig().getCookieName());
		logger.debug("Logout session (sid={}): {}", sessionId);
		try {
			Map<String, Object> logoutResponse = authService.logoutSession("", sessionId);
			String type = (logoutResponse != null) ? (String) logoutResponse.get("type") : null;
			if ("confirm".equals(type)) {
				model.addAttribute("logoutPrompt", true);
			} else if (logoutResponse != null && ("error".equals(type)
					|| logoutResponse.containsKey("error"))) {
				Object error = logoutResponse.containsKey("error_description")
						? logoutResponse.get("error_description") : logoutResponse.get("error");
				model.addAttribute("logoutError", error);
			} else {
				logger.debug("Logout session response: {}", logoutResponse);
			}
		} catch (IOException e) {
			logger.error("Logout session failed: {}", e.getMessage());
			model.addAttribute("logoutError", e.getMessage());
		}
		model.addAttribute("authenticatedUser", getAuthenticatedUser(request));
		return "home";
	}

	@PostMapping("/logout/confirm")
	public String logoutConfirm(HttpServletRequest request, HttpServletResponse response,
			Model model) throws IOException {
		String sessionId = CookieManager.getCookie(request, authService.getConfig().getCookieName());
		logger.debug("Logout session confirm (sid={}): {}", sessionId);
		Map<String, Object> logoutResponse = authService.logoutSessionConfirm(sessionId);
		if (logoutResponse != null && "end".equals(logoutResponse.get("type"))) {
			CookieManager.removeCookie(authService.getConfig().getCookieName(), response);
			String redirectUri = (String) logoutResponse.get("post_logout_redirect_uri");
			if (StringUtils.isNotBlank(redirectUri)) {
				return "redirect:" + redirectUri;
			}
			model.addAttribute("logoutEnd", true);
			return "home";
		}
		logger.debug("Logout session confirm response: {}", logoutResponse);
		return "redirect:/";
	}

	@PostMapping("/logout/cancel")
	public String logoutCancel() {
		return "redirect:/";
	}

	/**
	 * @param request HTTP request
	 * @return the data of the currently authenticated subject (end-user), or null if not present
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> getAuthenticatedUser(HttpServletRequest request) {
		try {
			String sessionId = CookieManager.getCookie(request, authService.getConfig().getCookieName());
			logger.debug("Submitting SID for verification (sid={}) to Connect2id server: {}", sessionId);
			Map<String, Object> session = authService.verifySubject(sessionId);
			if (session != null && session.containsKey("data")) {
				return (Map<String, Object>) session.get("data");
			}
		} catch (IOException e) {
			logger.error("Failed to verify subject session: {}", e.getMessage());
		}
		return null;
	}
}
