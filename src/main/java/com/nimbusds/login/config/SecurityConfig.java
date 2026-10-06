package com.nimbusds.login.config;

import com.nimbusds.login.web.FederatedLoginSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			FederatedLoginSuccessHandler successHandler,
			ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {
		http
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/",
								"/login/**",
								"/logout/**",
								"/consent/**",
								"/oauth2/**",
								"/err/**",
								"/error",
								"/actuator/health",
								"/webjars/**",
								"/css/**",
								"/js/**",
								"/images/**",
								"/favicon.ico",
								"/static/**")
						.permitAll()
						.anyRequest().permitAll())
				.formLogin(form -> form.disable())
				.logout(logout -> logout.disable())
				.httpBasic(basic -> basic.disable());
		if (clientRegistrations.getIfAvailable() != null) {
			http.oauth2Login(oauth -> oauth.loginPage("/login").successHandler(successHandler));
		}
		return http.build();
	}
}
