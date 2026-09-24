package com.nimbusds.login.web;

import com.nimbusds.login.rest.utils.CookieManager;
import com.nimbusds.login.service.C2idAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * Logout UI for RP-initiated and OpenID provider-initiated logout requests.
 */
@Controller
public class LogoutController {

	private final Logger logger = LoggerFactory.getLogger(LogoutController.class);
	private final C2idAuthService authService;

	public LogoutController(C2idAuthService authService) {
		this.authService = authService;
	}

	/**
	 * Starts an RP-initiated logout request received from the OpenID provider.
	 */
	@GetMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response, Model model) {
		return startLogout(request, response, model, false);
	}

	/**
	 * Supports RP-initiated logout requests submitted as form parameters.
	 */
	@PostMapping("/logout")
	public String logoutPost(HttpServletRequest request, HttpServletResponse response, Model model) {
		return startLogout(request, response, model, false);
	}

	/**
	 * Starts a provider-initiated logout from the local logout UI.
	 */
	@PostMapping("/logout/local")
	public String providerLogout(HttpServletRequest request, HttpServletResponse response, Model model) {
		return startLogout(request, response, model, true);
	}

	/**
	 * Sends the user's choice to the logout session API.
	 */
	@PostMapping("/logout/confirm")
	public String confirmLogout(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam String logoutSid, @RequestParam boolean opLogout) {
		try {
			Map<String, Object> logoutResponse = authService.logoutSessionConfirm(logoutSid, opLogout);
			return renderResponse(logoutResponse, response, model);
		} catch (IOException e) {
			logger.error("Logout confirmation failed: {}", e.getMessage());
			model.addAttribute("errorMessage", e.getMessage());
			return "logoutError";
		}
	}

	/**
	 * Abandons a logout request without calling the logout API.
	 */
	@PostMapping("/logout/cancel")
	public String cancelLogout() {
		return "redirect:/";
	}

	private String startLogout(HttpServletRequest request, HttpServletResponse response,
			Model model, boolean providerInitiated) {
		String sessionId = CookieManager.getCookie(request, authService.getConfig().getCookieName());
		String query = providerInitiated ? null : requestParameters(request);
		try {
			Map<String, Object> logoutResponse = authService.logoutSession(query, sessionId,
					providerInitiated ? "OP" : null);
			return renderResponse(logoutResponse, response, model);
		} catch (IOException e) {
			logger.error("Logout request failed: {}", e.getMessage());
			model.addAttribute("errorMessage", e.getMessage());
			return "logoutError";
		}
	}

	private String renderResponse(Map<String, Object> logoutResponse, HttpServletResponse response, Model model) {
		if (logoutResponse == null) {
			model.addAttribute("errorMessage", "The logout server returned no response.");
			return "logoutError";
		}

		String type = String.valueOf(logoutResponse.get("type"));
		if ("confirm".equals(type)) {
			model.addAttribute("logoutSid", logoutResponse.get("sid"));
			model.addAttribute("authenticatedUser", sessionData(logoutResponse));
			Map<String, Object> client = nestedMap(logoutResponse, "client");
			model.addAttribute("clientName", client != null ? client.get("name") : null);
			model.addAttribute("idTokenHintPresent", booleanValue(logoutResponse,
					"id_token_hint_present", "id_token_hint_presented"));
			return "logout";
		}

		if ("end".equals(type)) {
			boolean sessionClosed = Boolean.TRUE.equals(logoutResponse.get("sub_session_closed"));
			if (sessionClosed && response != null) {
				CookieManager.removeCookie(authService.getConfig().getCookieName(), response);
			}
			model.addAttribute("sessionClosed", sessionClosed);
			model.addAttribute("redirectUrl", logoutResponse.get("post_logout_redirect_uri"));
			model.addAttribute("frontChannelLogoutUrls", frontChannelLogoutUrls(logoutResponse));
			return "logoutEnd";
		}

		if ("error".equals(type) || logoutResponse.containsKey("error")) {
			Object errorDescription = logoutResponse.get("error_description");
			model.addAttribute("errorMessage", errorDescription != null
					? errorDescription : "The logout request could not be processed.");
			return "logoutError";
		}

		model.addAttribute("errorMessage", "Unexpected logout response.");
		return "logoutError";
	}

	private String requestParameters(HttpServletRequest request) {
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			return request.getQueryString();
		}

		List<String> parameters = new ArrayList<>();
		request.getParameterMap().forEach((name, values) -> {
			for (String value : values) {
				parameters.add(URLEncoder.encode(name, StandardCharsets.UTF_8)
						+ "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
			}
		});
		return parameters.isEmpty() ? null : String.join("&", parameters);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> nestedMap(Map<String, Object> source, String... path) {
		Map<String, Object> current = source;
		for (String key : path) {
			if (current == null || !(current.get(key) instanceof Map)) {
				return null;
			}
			current = (Map<String, Object>) current.get(key);
		}
		return current;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> sessionData(Map<String, Object> logoutResponse) {
		Map<String, Object> session = nestedMap(logoutResponse, "sub_session");
		if (session == null || !(session.get("data") instanceof Map)) {
			return null;
		}
		return (Map<String, Object>) session.get("data");
	}

	private List<String> frontChannelLogoutUrls(Map<String, Object> logoutResponse) {
		Object values = logoutResponse.get("frontchannel_logout_uris");
		if (!(values instanceof List<?>)) {
			return Collections.emptyList();
		}
		List<String> urls = new ArrayList<>();
		for (Object value : (List<?>) values) {
			if (value != null && StringUtils.isNotBlank(value.toString())) {
				urls.add(value.toString());
			}
		}
		return urls;
	}

	private boolean booleanValue(Map<String, Object> values, String... keys) {
		for (String key : keys) {
			Object value = values.get(key);
			if (value instanceof Boolean) {
				return (Boolean) value;
			}
		}
		return false;
	}
}
