package com.nimbusds.login.rest.errors;

import com.nimbusds.login.web.Messages;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Controller advice to translate the server side exceptions to client-friendly structures, or to
 * the error page for regular page requests.
 */
@ControllerAdvice
public class ExceptionTranslator {

	private final Logger logger = LoggerFactory.getLogger(ExceptionTranslator.class);
	private final Messages messages;

	public static final String ERR_CONCURRENCY_FAILURE = "error.concurrencyFailure";
	public static final String ERR_ACCESS_DENIED = "error.accessDenied";
	public static final String ERR_VALIDATION = "error.validation";
	public static final String ERR_METHOD_NOT_SUPPORTED = "error.methodNotSupported";
	public static final String ERR_INTERNAL_SERVER_ERROR = "error.internalServerError";

	public ExceptionTranslator(Messages messages) {
		this.messages = messages;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorVM processValidationError(MethodArgumentNotValidException ex) {
		BindingResult result = ex.getBindingResult();
		List<FieldError> fieldErrors = result.getFieldErrors();

		return processFieldErrors(fieldErrors);
	}

	@ExceptionHandler(CustomParameterizedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ParameterizedErrorVM processParameterizedValidationError(CustomParameterizedException ex) {
		return ex.getErrorVM();
	}

	private ErrorVM processFieldErrors(List<FieldError> fieldErrors) {
		ErrorVM dto = new ErrorVM(ERR_VALIDATION);

		for (FieldError fieldError : fieldErrors) {
			dto.add(fieldError.getObjectName(), fieldError.getField(), fieldError.getCode());
		}

		return dto;
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	public ErrorVM processMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
		return new ErrorVM(ERR_METHOD_NOT_SUPPORTED, exception.getMessage());
	}

	/**
	 * Spring MVC raises this exception when a request does not match a controller and no static
	 * resource exists. These requests are expected for probes and stale browser assets, so they
	 * must be returned as ordinary 404 responses instead of being logged as internal errors.
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorVM processResourceNotFoundException(NoResourceFoundException exception) {
		return new ErrorVM("error.url.not.found", exception.getResourcePath());
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public String processInvalidCredentialsException(InvalidCredentialsException ex, Model model) {
		model.addAttribute("msg", messages);
		model.addAttribute("errorMessage", "Invalid credentials.");
		return "error";
	}

	@ExceptionHandler(Exception.class)
	public String processRuntimeException(Exception ex, Model model) {
		model.addAttribute("msg", messages);
		ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
		if (responseStatus != null) {
			model.addAttribute("errorMessage", responseStatus.reason());
		} else {
			model.addAttribute("errorMessage", messages.get(ERR_INTERNAL_SERVER_ERROR));
		}
		logger.error(model.getAttribute("errorMessage").toString(), ex);
		return "error";
	}
}
