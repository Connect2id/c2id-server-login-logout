package com.nimbusds.login.web;

import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.service.C2idAuthService;
import com.nimbusds.login.spi.FederatedSubjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Bridges any configured Spring Security OAuth 2.0 / OIDC client into a pending
 * Connect2id authorisation session.
 */
@Controller
public class FederatedLoginController {

	private final Logger logger = LoggerFactory.getLogger(FederatedLoginController.class);

	static final String AUTH_SESSION_ATTRIBUTE = "federatedAuthSessionId";
	private static final String PROVIDER_ATTRIBUTE = "federatedProvider";

	private final C2idAuthService authService;
	private final AuthzResponseDispatcher dispatcher;
	private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;
	private final FederatedSubjectResolver subjectResolver;

	public FederatedLoginController(C2idAuthService authService, AuthzResponseDispatcher dispatcher,
			ObjectProvider<ClientRegistrationRepository> clientRegistrations,
			FederatedSubjectResolver subjectResolver) {
		this.authService = authService;
		this.dispatcher = dispatcher;
		this.clientRegistrations = clientRegistrations;
		this.subjectResolver = subjectResolver;
	}

	@GetMapping("/login/federated/{registrationId}")
	public String start(@PathVariable String registrationId, @RequestParam String authSessionId,
			HttpSession session) {
		ClientRegistrationRepository repository = clientRegistrations.getIfAvailable();
		String pendingSessionId = pendingSessionId(session, authSessionId);
		if (StringUtils.isBlank(pendingSessionId) || repository == null
				|| repository.findByRegistrationId(registrationId) == null) {
			return "redirect:/";
		}
		session.setAttribute(AUTH_SESSION_ATTRIBUTE, pendingSessionId);
		session.setAttribute(PROVIDER_ATTRIBUTE, registrationId);
		return "redirect:/oauth2/authorization/" + registrationId;
	}

	@GetMapping("/login/federated/complete")
	public String complete(HttpServletRequest request, HttpServletResponse response, Model model,
			HttpSession session, Authentication authentication,
			@AuthenticationPrincipal OAuth2User user) throws AuthenticationException {
		String authSessionId = (String) session.getAttribute(AUTH_SESSION_ATTRIBUTE);
		String provider = (String) session.getAttribute(PROVIDER_ATTRIBUTE);
		session.removeAttribute(AUTH_SESSION_ATTRIBUTE);
		session.removeAttribute(PROVIDER_ATTRIBUTE);
		if (user == null || StringUtils.isBlank(authSessionId) || StringUtils.isBlank(provider)
				|| !(authentication instanceof OAuth2AuthenticationToken oauthAuthentication)
				|| !provider.equals(oauthAuthentication.getAuthorizedClientRegistrationId())) {
			return "redirect:/";
		}
		try {
			Map<String, Object> authzResponse = authService.
					authenticateFederatedSubject(subjectResolver.resolve(authentication), authSessionId);
			return dispatcher.dispatch(authzResponse, request, response, model);
		} catch (AuthenticationException e) {
			logger.info("Authentication failed for user coming from '{}': {}", provider, e.getMessage());
			return loginError(model, request, provider);
		} catch (IOException e) {
			model.addAttribute("errorMessage", "Federated authentication failed.");
			return "logoutError";
		}
	}

	private String loginError(Model model, HttpServletRequest request, String provider) {
		model.addAttribute("authenticationError", true);
		model.addAttribute("actionUrl", Utils.currentUrl(request));
		model.addAttribute("cancelUrl", Utils.cancelUrl(request));
		model.addAttribute("authSessionId", request.getParameter("authSessionId"));
		Utils.populatePrompt(model, Utils.authPrompt(request),
				Boolean.TRUE.equals(request.getSession().getAttribute("newAccountLogin")));
		return "login";
	}

	private String pendingSessionId(HttpSession session, String requestSessionId) {
		if (StringUtils.isBlank(requestSessionId)) {
			return null;
		}
		Object prompt = session.getAttribute("c2idAuthPrompt");
		if (prompt instanceof Map<?, ?> authPrompt && authPrompt.get("sid") != null) {
			String promptSessionId = String.valueOf(authPrompt.get("sid"));
			return requestSessionId.equals(promptSessionId) ? requestSessionId : null;
		}
		return requestSessionId;
	}
}
