package com.nimbusds.login.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Error page controller, replacing the redirect to the error view.
 * The error message travels in the URL path, Base64 encoded.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class ErrorPageController implements ErrorController {

	private final Messages messages;

	/**
	 * Creates a new ErrorPageController with the given messages utility.
	 *
	 * @param messages the messages utility for adding template attributes
	 */
	public ErrorPageController(Messages messages) {
		this.messages = messages;
	}

	/**
	 * Handles the Spring Boot error dispatch path and renders the error page directly.
	 *
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @return the name of the error view
	 */
	@RequestMapping("/error")
	public String errorPage(HttpServletRequest request, Model model) {
		messages.addTemplateAttributes(model, request);
		return "error";
	}

	/**
	 * Renders an error page for a specific error message encoded in the URL path.
	 *
	 * @param msg the URL-safe Base64 encoded error message
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @return the name of the error view
	 */
	@GetMapping("/err/{msg}")
	public String errorMessage(@PathVariable String msg, HttpServletRequest request, Model model) {
		messages.addTemplateAttributes(model, request);
		model.addAttribute("errorMessage", ErrorMessages.decode(msg));
		return "error";
	}

	/**
	 * Renders a generic error page without a specific error message.
	 *
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @return the name of the error view
	 */
	@GetMapping("/err")
	public String error(HttpServletRequest request, Model model) {
		messages.addTemplateAttributes(model, request);
		return "error";
	}
}