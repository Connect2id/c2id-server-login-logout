package com.nimbusds.login.web.errors;

import com.nimbusds.login.auth.InvalidCredentialsException;
import com.nimbusds.login.web.Messages;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Renders server-side error pages for MVC requests.
 *
 * <p>This controller advice handles various exception types and renders
 * appropriate error pages with meaningful error messages. Each handled
 * exception maps to a specific error view and status code.</p>
 */
@ControllerAdvice
public class ExceptionTranslator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionTranslator.class);

	private static final String ERR_VALIDATION = "error.validation";
	private static final String ERR_METHOD_NOT_SUPPORTED = "error.methodNotSupported";
	private static final String ERR_URL_NOT_FOUND = "error.url.not.found";
	private static final String ERR_INTERNAL_SERVER_ERROR = "error.internalServerError";

	private final Messages messages;

	/**
	 * Creates a new ExceptionTranslator with the given messages utility.
	 *
	 * @param messages the messages utility for retrieving error message keys and adding template attributes
	 */
	public ExceptionTranslator(Messages messages) {
		this.messages = messages;
	}

	/**
	 * Handles invalid credentials exceptions and renders an authentication error page.
	 *
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @return the name of the error view with UNAUTHORIZED status
	 */
	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public String processInvalidCredentialsException(HttpServletRequest request, Model model) {
		return renderError(request, model, "error.authentication");
	}

	/**
	 * Handles method argument validation exceptions and renders a validation error page.
	 *
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @return the name of the error view with BAD_REQUEST status
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String processValidationError(HttpServletRequest request, Model model) {
		return renderError(request, model, ERR_VALIDATION);
	}

	/**
	 * Handles HTTP method not supported exceptions and renders a method not supported error page.
	 *
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @return the name of the error view with METHOD_NOT_ALLOWED status
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	public String processMethodNotSupportedException(HttpServletRequest request, Model model) {
		return renderError(request, model, ERR_METHOD_NOT_SUPPORTED);
	}

	/**
	 * Handles resource not found exceptions and renders a URL not found error page.
	 *
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @return the name of the error view with NOT_FOUND status
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String processResourceNotFoundException(HttpServletRequest request, Model model) {
		return renderError(request, model, ERR_URL_NOT_FOUND);
	}

	/**
	 * Handles runtime exceptions and renders an internal server error page.
	 *
	 * @param exception the exception that was thrown
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @return the name of the error view
	 */
	@ExceptionHandler(Exception.class)
	public String processRuntimeException(Exception exception, HttpServletRequest request, Model model) {
		ResponseStatus responseStatus = AnnotationUtils.findAnnotation(exception.getClass(), ResponseStatus.class);
		String messageKey = responseStatus != null && !responseStatus.reason().isBlank()
				? responseStatus.reason() : ERR_INTERNAL_SERVER_ERROR;
		LOGGER.error(messages.get(messageKey), exception);
		return renderError(request, model, messageKey);
	}

	/**
	 * Renders an error page with the given message key.
	 *
	 * @param request the HTTP servlet request
	 * @param model the model to add attributes to
	 * @param messageKey the message key to retrieve the error message from the messages utility
	 * @return the name of the error view
	 */
	private String renderError(HttpServletRequest request, Model model, String messageKey) {
		messages.addTemplateAttributes(model, request);
		model.addAttribute("errorMessage", messages.get(messageKey));
		return "error";
	}
}