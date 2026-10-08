package com.nimbusds.login.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Ensures every HTML form rendered by the application includes a CSRF token,
 * and provides the current provider list for federated login.
 */
@ControllerAdvice
public class CsrfControllerAdvice {

	/**
	 * Repository of client registrations for OAuth2/OpenID Connect providers.
	 */
	private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

	/**
	 * Creates a new CSRF advice with the given client registration repository provider.
	 *
	 * @param clientRegistrations provider of the {@link ClientRegistrationRepository}
	 */
	public CsrfControllerAdvice(ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
		this.clientRegistrations = clientRegistrations;
	}

	/**
	 * Adds the current CSRF token as a model attribute named "csrfToken".
	 * This ensures every HTML form rendered by the application includes a CSRF token.
	 *
	 * @param request the HTTP servlet request to extract the CSRF token from
	 * @return the CSRF token value, or empty string if not present
	 */
	@ModelAttribute("csrfToken")
	public String csrfToken(HttpServletRequest request) {
		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		return csrfToken != null ? csrfToken.getToken() : "";
	}

	/**
	 * Adds the current list of federated login providers as a model attribute named "federatedProviders".
	 * Returns an empty list if no client registration repository is available.
	 *
	 * @return list of provider maps with {@code registrationId} and {@code clientName}, or empty list
	 */
	@ModelAttribute("federatedProviders")
	public List<Map<String, String>> federatedProviders() {
		ClientRegistrationRepository repository = clientRegistrations.getIfAvailable();
		if (!(repository instanceof Iterable<?> registrations)) {
			return Collections.emptyList();
		}
		List<Map<String, String>> providers = new ArrayList<>();
		for (Object registration : registrations) {
			if (registration instanceof ClientRegistration client) {
				Map<String, String> provider = new HashMap<>();
				provider.put("registrationId", client.getRegistrationId());
				provider.put("clientName", client.getClientName());
				providers.add(provider);
			}
		}
		return providers;
	}
}