package com.nimbusds.login.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * App configuration properties. Properties are configured in the application.yml file.
 */
@ConfigurationProperties(prefix = "c2id", ignoreUnknownFields = false)
public class ApplicationProperties {

	private final Authz authz = new Authz();
	private final Session session = new Session();
	private final LdapAuth ldapAuth = new LdapAuth();
	private final Logout logout = new Logout();
	public String endpoint;
	public String cookieName; // session id cookie
	public String authenticatorClass;

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
		private String redirectUri;

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

		public String getRedirectUri() {
			return redirectUri;
		}

		public void setRedirectUri(String redirectUri) {
			this.redirectUri = redirectUri;
		}
	}
}
