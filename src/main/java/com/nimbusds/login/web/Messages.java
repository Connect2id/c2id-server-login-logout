package com.nimbusds.login.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
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

	@ModelAttribute("contextPath")
	public String contextPath(HttpServletRequest request) {
		return request.getContextPath();
	}

	@ModelAttribute("imagesPath")
	public String imagesPath(HttpServletRequest request) {
		return request.getContextPath() + "/img";
	}

	@ModelAttribute("stylesPath")
	public String stylesPath(HttpServletRequest request) {
		return request.getContextPath() + "/css";
	}

	@ModelAttribute("scriptsPath")
	public String scriptsPath(HttpServletRequest request) {
		return request.getContextPath() + "/js";
	}

	/**
	 * Adds the common JTE layout attributes to a model. This is also used by
	 * error dispatches, where controller-advice model methods may not run.
	 */
	public void addTemplateAttributes(Model model, HttpServletRequest request) {
		model.addAttribute("msg", this);
		model.addAttribute("contextPath", contextPath(request));
		model.addAttribute("imagesPath", imagesPath(request));
		model.addAttribute("stylesPath", stylesPath(request));
		model.addAttribute("scriptsPath", scriptsPath(request));
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
