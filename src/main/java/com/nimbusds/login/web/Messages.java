package com.nimbusds.login.web;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the Spring {@link MessageSource} to the JTE templates as the "msg" model attribute,
 * resolving the message for the current request locale.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@ControllerAdvice
public class Messages {

	private final MessageSource messageSource;

	public Messages(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@ModelAttribute("msg")
	public Messages messages() {
		return this;
	}

	/**
	 * @param key the message key, for example "login.title"
	 * @return the resolved message, or the key itself if no message is found
	 */
	public String get(String key) {
		return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
	}

	/**
	 * @param key the message key
	 * @param args the message arguments
	 * @return the resolved message, or the key itself if no message is found
	 */
	public String get(String key, Object... args) {
		return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
	}
}
