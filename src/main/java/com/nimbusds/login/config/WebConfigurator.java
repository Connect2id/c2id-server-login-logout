package com.nimbusds.login.config;

import com.nimbusds.login.spi.FederatedSubjectResolver;
import com.nimbusds.login.spi.UserAuthenticator;
import com.nimbusds.login.spi.impl.ExampleSubjectResolver;
import com.nimbusds.login.spi.impl.LdapUserAuthenticator;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.util.ServiceLoader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
public class WebConfigurator implements ServletContextInitializer,
		WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

	private final Logger logger = LoggerFactory.getLogger(WebConfigurator.class);
	private final Environment env;

	public WebConfigurator(Environment env) {
		this.env = env;
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		if (env.getActiveProfiles().length != 0) {
			logger.debug("Web application configuration, using profiles: {}", (Object[]) env.getActiveProfiles());
		}
	}

	/**
	 * Customize the Servlet engine: Mime types.
	 */
	@Override
	public void customize(ConfigurableServletWebServerFactory container) {
		MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
		// IE issue
		mappings.add("html", "text/html;charset=utf-8");
		container.setMimeMappings(mappings);
	}

	@Bean
	public UserAuthenticator userAuthenticator(ApplicationProperties config) {
		String authenticatorClass = config.getAuthenticatorClass();
		if (!StringUtils.isBlank(authenticatorClass)) {
			for (UserAuthenticator authImpl : ServiceLoader.load(UserAuthenticator.class)) {
				if (authImpl != null && authenticatorClass.equalsIgnoreCase(authImpl.getClass().getSimpleName())) {
					logger.info("Using UserAuthenticator implementation: {}", authImpl.getClass().getName());
					return authImpl;
				}
			}
			logger.warn("No UserAuthenticator implementation found for '{}', using {}",
					authenticatorClass, LdapUserAuthenticator.class.getName());
		}
		logger.info("Using UserAuthenticator implementation: {}", LdapUserAuthenticator.class.getName());
		return new LdapUserAuthenticator(config);
	}

	@Bean
	public FederatedSubjectResolver subjectResolver(ApplicationProperties config) {
		String resolverClass = config.getAuthenticatorClass();
		if (!StringUtils.isBlank(resolverClass)) {
			for (FederatedSubjectResolver resolverImpl : ServiceLoader.load(FederatedSubjectResolver.class)) {
				if (resolverImpl != null && resolverClass.equalsIgnoreCase(resolverImpl.getClass().getSimpleName())) {
					logger.info("Using FederatedSubjectResolver implementation: {}", resolverImpl.getClass().getName());
					return resolverImpl;
				}
			}
			logger.warn("No FederatedSubjectResolver implementation found for '{}', using {}",
					resolverClass, ExampleSubjectResolver.class.getName());
		}
		logger.info("Using FederatedSubjectResolver implementation: {}", ExampleSubjectResolver.class.getName());
		return new ExampleSubjectResolver(config);
	}
}
