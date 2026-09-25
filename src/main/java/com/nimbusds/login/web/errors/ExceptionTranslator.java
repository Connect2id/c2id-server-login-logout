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
 */
@ControllerAdvice
public class ExceptionTranslator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionTranslator.class);

	private static final String ERR_VALIDATION = "error.validation";
	private static final String ERR_METHOD_NOT_SUPPORTED = "error.methodNotSupported";
	private static final String ERR_URL_NOT_FOUND = "error.url.not.found";
	private static final String ERR_INTERNAL_SERVER_ERROR = "error.internalServerError";

	private final Messages messages;

	public ExceptionTranslator(Messages messages) {
		this.messages = messages;
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public String processInvalidCredentialsException(HttpServletRequest request, Model model) {
		return renderError(request, model, "error.authentication");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String processValidationError(HttpServletRequest request, Model model) {
		return renderError(request, model, ERR_VALIDATION);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	public String processMethodNotSupportedException(HttpServletRequest request, Model model) {
		return renderError(request, model, ERR_METHOD_NOT_SUPPORTED);
	}

	/**
	 * Handles missing controllers and static resources as ordinary HTML errors.
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String processResourceNotFoundException(HttpServletRequest request, Model model) {
		return renderError(request, model, ERR_URL_NOT_FOUND);
	}

	@ExceptionHandler(Exception.class)
	public String processRuntimeException(Exception exception, HttpServletRequest request, Model model) {
		ResponseStatus responseStatus = AnnotationUtils.findAnnotation(exception.getClass(), ResponseStatus.class);
		String messageKey = responseStatus != null && !responseStatus.reason().isBlank()
				? responseStatus.reason() : ERR_INTERNAL_SERVER_ERROR;
		LOGGER.error(messages.get(messageKey), exception);
		return renderError(request, model, messageKey);
	}

	private String renderError(HttpServletRequest request, Model model, String messageKey) {
		messages.addTemplateAttributes(model, request);
		model.addAttribute("errorMessage", messages.get(messageKey));
		return "error";
	}
}
