package com.nimbusds.login;

import com.nimbusds.login.config.ApplicationProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
public class C2idLogin extends SpringBootServletInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(C2idLogin.class);

	public C2idLogin() {
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(C2idLogin.class);
	}

	/**
	 * Main method, used to run the application.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(C2idLogin.class);
		app.setWebApplicationType(WebApplicationType.SERVLET);
		ConfigurableApplicationContext context = app.run(args);

		if (context instanceof WebServerApplicationContext webServerContext) {
			Environment environment = context.getEnvironment();
			String scheme = Boolean.parseBoolean(environment.getProperty("server.ssl.enabled", "false"))
				? "https" : "http";
			String address = environment.getProperty("server.address", "localhost");
			String contextPath = environment.getProperty("server.servlet.context-path", "");
			int port = webServerContext.getWebServer().getPort();

			LOGGER.info("Application is available at {}://{}:{}{}", scheme, address, port, contextPath);
		}
	}
}
