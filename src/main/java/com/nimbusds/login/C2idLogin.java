package com.nimbusds.login;

import com.nimbusds.login.config.ApplicationProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
public class C2idLogin extends SpringBootServletInitializer {

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
		app.run(args);
	}
}
