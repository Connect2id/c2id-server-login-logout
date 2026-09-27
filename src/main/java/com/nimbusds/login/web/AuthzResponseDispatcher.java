package com.nimbusds.login.web;

import com.nimbusds.login.rest.utils.SessionCookieManager;
import com.nimbusds.login.service.C2idAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Handles the response of the Connect2id server after every authorisation session interaction.
 * Depending on the response type it either shows the login form, shows the consent form, relays
 * the final OpenID authentication response back to the client, or shows an error page.
 * It also updates the subject session id cookie in the same way the old REST layer did.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
public class AuthzResponseDispatcher {

	private final Logger logger = LoggerFactory.getLogger(AuthzResponseDispatcher.class);
	private final C2idAuthService authService;
	private final SessionCookieManager sessionCookieManager;

	public AuthzResponseDispatcher(C2idAuthService authService, SessionCookieManager sessionCookieManager) {
		this.authService = authService;
		this.sessionCookieManager = sessionCookieManager;
	}

	/**
	 * @param authzResponse the response object from the Connect2id server
	 * @param request HTTP request
	 * @param response HTTP response
	 * @param model the MVC model for the form_post view
	 * @return a view name or a redirect URL for the given Connect2id response
	 */
	@SuppressWarnings("unchecked")
	public String dispatch(Map<String, Object> authzResponse, HttpServletRequest request,
			HttpServletResponse response, Model model) {
		String type = (authzResponse != null) ? (String) authzResponse.get("type") : null;
		logger.debug("Received '{}' message: {}", type, authzResponse);

		if ("auth".equals(type)) {
			updateSessionIdCookie(authzResponse, request, response);
			request.getSession().setAttribute("c2idAuthPrompt", authzResponse);
			// The authorisation session was created, show the login form
			return "redirect:/login?authSessionId=" + urlEncode((String) authzResponse.get("sid"))
					+ originalQuerySuffix(request);
		} else if ("consent".equals(type)) {
			updateSessionIdCookie(authzResponse, request, response);
			// The subject was authenticated, remember the response and show the consent form
			request.getSession().setAttribute("c2idAuthzResponse", authzResponse);
			return "redirect:/consent?authSessionId=" + urlEncode((String) authzResponse.get("sid"))
					+ originalQuerySuffix(request);
		} else if ("response".equals(type)) {
			updateSessionIdCookie(authzResponse, request, response);
			// Relays the final OpenID authentication response back to the client
			Map<String, Object> parameters = (Map<String, Object>) authzResponse.get("parameters");
			String mode = (String) authzResponse.get("mode");
			if (parameters != null && ("query".equals(mode) || "fragment".equals(mode)
					|| "query.jwt".equals(mode) || "fragment.jwt".equals(mode))) {
				logger.debug("Redirection URI: {}", parameters.get("uri"));
				return "redirect:" + parameters.get("uri");
			} else if (parameters != null && ("form_post".equals(mode) || "form_post.jwt".equals(mode))) {
				model.addAttribute("uri", parameters.get("uri"));
				model.addAttribute("form", parameters.get("form"));
				return "formPost";
			}
			return "redirect:/err/" + ErrorMessages.encode((String) authzResponse.get("error_description"));
		}
		// The Connect2id server returned an error
		return "redirect:/err/" + ErrorMessages.encode((String)
				(authzResponse != null ? authzResponse.get("error_description") : null));
	}

	/**
	 * Appends the query string of the original OpenID authentication request (stashed in the
	 * HTTP session) to a redirect URL, so that the login and consent pages keep the client
	 * parameters, like "redirect_uri" and "state".
	 */
	private String originalQuerySuffix(HttpServletRequest request) {
		String queryString = (String) request.getSession().getAttribute("c2idQueryString");
		return StringUtils.isBlank(queryString) ? "" : "&" + queryString;
	}

	private String urlEncode(String value) {
		return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	/**
	 * Sets, updates or removes the subject session id cookie, depending on the response type.
	 */
	@SuppressWarnings("unchecked")
	private void updateSessionIdCookie(Map<String, Object> authzResponse,
			HttpServletRequest request, HttpServletResponse response) {
		if (authzResponse != null && authzResponse.containsKey("type")) {
			if ("auth".equals(authzResponse.get("type"))) {
				// Keep existing account sessions available for account switching.
			} else if ("consent".equals(authzResponse.get("type")) && authzResponse.containsKey("sub_session")) {
				Map<String, Object> sub = (Map<String, Object>) authzResponse.get("sub_session");
				String cookieValue = (String) sub.get("sid");
				if (!StringUtils.isBlank(cookieValue)) {
					sessionCookieManager.activateSession(request, response,
							authService.getConfig().getCookieName(), cookieValue);
				}
			} else if ("response".equals(authzResponse.get("type"))) {
				String cookieValue = (String) authzResponse.get("sub_sid");
				if (!StringUtils.isBlank(cookieValue)) {
					sessionCookieManager.activateSession(request, response,
						authService.getConfig().getCookieName(), cookieValue);
				}
			}
		}
	}
}
