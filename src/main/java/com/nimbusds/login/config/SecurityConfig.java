package com.nimbusds.login.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/",
								"/login/**",
								"/logout/**",
								"/consent/**",
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
		return http.build();
	}
}
