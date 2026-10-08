package com.nimbusds.login.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * App configuration properties. Properties are configured in the application.yml file.
 */
@ConfigurationProperties(prefix = "c2id", ignoreUnknownFields = false)
public class ApplicationProperties {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationProperties.class);

	private final Authz authz = new Authz();
	private final Session session = new Session();
	private final LdapAuth ldapAuth = new LdapAuth();
	private final Logout logout = new Logout();
	public String endpoint;
	public String cookieName; // session id cookie
	public String authenticatorClass;
	public String subjectResolverClass;

	public Authz getAuthz() {
		return authz;
	}

	public Session getSession() {
		return session;
	}

	public LdapAuth getLdapAuth() {
		return ldapAuth;
	}

	public Logout getLogout() {
		return logout;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getCookieName() {
		return cookieName;
	}

	public void setCookieName(String cookieName) {
		this.cookieName = cookieName;
	}

	public String getAuthenticatorClass() {
		return authenticatorClass;
	}

	public void setAuthenticatorClass(String authenticatorClass) {
		this.authenticatorClass = authenticatorClass;
	}

	public String getSubjectResolverClass() {
		return subjectResolverClass;
	}

	public void setSubjectResolverClass(String subjectResolverClass) {
		this.subjectResolverClass = subjectResolverClass;
	}

	/**
	 * Logs the active non-secret Connect2id configuration after Spring Boot binds it.
	 * Access tokens and API keys are intentionally not logged.
	 */
	@PostConstruct
	void logConfiguration() {
		logger.info("Currently active configuration:");
		log("c2id.endpoint", endpoint);
		log("c2id.cookieName", cookieName);
		log("c2id.authenticatorClass", authenticatorClass);
		log("c2id.subjectResolverClass", subjectResolverClass);
		log("c2id.authz.resourcePath", authz.getResourcePath());
		log("c2id.authz.longLived", authz.isLongLived());
		log("c2id.session.resourcePath", session.getResourcePath());
		log("c2id.ldapAuth.endpoint", ldapAuth.getEndpoint());
		log("c2id.logout.resourcePath", logout.getResourcePath());
		log("c2id.logout.defaultPostLogoutRedirectUri", logout.getDefaultPostLogoutRedirectUri());
	}

	private void log(String name, Object value) {
		logger.info("  \u001B[1m{}\u001B[0m=\u001B[36m{}\u001B[0m", name, value == null ? "" : value);
	}

	public boolean isDemoMode() {
		return getAuthz().getAccessToken().equals("ztucZS1ZyFKgh0tUEruUtiSTXhnexmd6");
	}

	public static class Authz {

		private String resourcePath;
		private String accessToken;
        private boolean longLived;

		public String getResourcePath() {
			return resourcePath;
		}

		public void setResourcePath(String resourcePath) {
			this.resourcePath = resourcePath;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

        public boolean isLongLived() {
            return longLived;
        }

        public void setLongLived(boolean longLived) {
            this.longLived = longLived;
        }
	}

	public static class Session {

		private String resourcePath;
		private String accessToken;

		public String getResourcePath() {
			return resourcePath;
		}

		public void setResourcePath(String resourcePath) {
			this.resourcePath = resourcePath;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}
	}

	public static class LdapAuth {

		private String endpoint;
		private String apiKey;

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}
	}

	public static class Logout {

		private String resourcePath;
		private String accessToken;
		private String defaultPostLogoutRedirectUri;

		public String getResourcePath() {
			return resourcePath;
		}

		public void setResourcePath(String resourcePath) {
			this.resourcePath = resourcePath;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		public String getDefaultPostLogoutRedirectUri() {
			return defaultPostLogoutRedirectUri;
		}

		public void setDefaultPostLogoutRedirectUri(String defaultPostLogoutRedirectUri) {
			this.defaultPostLogoutRedirectUri = defaultPostLogoutRedirectUri;
		}
	}
}
