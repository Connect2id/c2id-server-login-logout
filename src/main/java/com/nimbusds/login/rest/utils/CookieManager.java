package com.nimbusds.login.rest.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * Sets and removes cookies.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class CookieManager {

	private CookieManager() {
	}

	/**
	 * Reads a cookie.
	 *
	 * @param request HTTP request
	 * @param name the name of the cookie
	 * @return the cookie value or null
	 */
	public static String getCookie(final HttpServletRequest request, final String name) {
		if (StringUtils.isBlank(name) || request == null) {
			return null;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return null;
		}
		//Otherwise, we have to do a linear scan for the cookie.
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	/**
	 * Set a cookie using the specified HTTP servlet response.
	 *
	 * @param cookieName The cookie name.
	 * @param cookieValue The cookie value.
	 * @param request The HTTP servlet request to use.
	 * @param response The HTTP servlet response to use.
	 * @param httpOnly HTTP-only flag
	 * @param maxAgeSeconds maximum valid period of the cookie, in seconds
	 */
	public static void addCookie(final String cookieName, final String cookieValue, HttpServletRequest request,
			final HttpServletResponse response, boolean httpOnly, int maxAgeSeconds) {
		Cookie cookie = new Cookie(cookieName, cookieValue);
		cookie.setPath("/");
		cookie.setMaxAge(maxAgeSeconds);
		cookie.setHttpOnly(httpOnly);
		cookie.setSecure(request.isSecure());
		response.addCookie(cookie);
	}

	/**
	 * Remove a cookie using the specified HTTP servlet response.
	 *
	 * @param cookieName The cookie name.
	 * @param response The HTTP servlet response to use.
	 */
	public static void removeCookie(final String cookieName, final HttpServletResponse response) {
		Cookie cookie = new Cookie(cookieName, "");
		cookie.setMaxAge(0);
		cookie.setPath("/");
		response.addCookie(cookie);
	}

}
