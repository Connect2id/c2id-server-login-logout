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

@ControllerAdvice
public class CsrfControllerAdvice {

	private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

	public CsrfControllerAdvice(ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
		this.clientRegistrations = clientRegistrations;
	}

	@ModelAttribute("csrfToken")
	public String csrfToken(HttpServletRequest request) {
		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		return csrfToken != null ? csrfToken.getToken() : "";
	}

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
