package com.nimbusds.login.web;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Error page controller, replacing the redirect to the error view of the previous AngularJS
 * front-end. The error message travels in the URL path, Base64 encoded.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class ErrorPageController implements ErrorController {

	private final Messages messages;

	public ErrorPageController(Messages messages) {
		this.messages = messages;
	}

	@GetMapping("/err/{msg}")
	public String errorMessage(@PathVariable String msg, Model model) {
		model.addAttribute("msg", messages);
		model.addAttribute("errorMessage", ErrorMessages.decode(msg));
		return "error";
	}

	@GetMapping("/err")
	public String error(Model model) {
		model.addAttribute("msg", messages);
		return "error";
	}

	/**
	 * Handles the Spring Boot error dispatch path and renders the error page directly.
	 */
	@RequestMapping("/error")
	public String errorPage(Model model) {
		model.addAttribute("msg", messages);
		return "error";
	}
}
