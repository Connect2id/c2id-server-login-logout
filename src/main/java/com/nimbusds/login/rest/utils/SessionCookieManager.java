package com.nimbusds.login.rest.utils;

import com.nimbusds.login.config.ApplicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Maintains the multiple Connect2id subject sessions used for account switching.
 */
@Component
public class SessionCookieManager {

	private static final String SLOT_PREFIX_SEPARATOR = "-";
	private static final String SLOT_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int SLOT_LENGTH = 2;
	private static final int SESSION_MAX_AGE = 60 * 60 * 24 * 30;

	private final ApplicationProperties config;

	private final SecureRandom random = new SecureRandom();

	public SessionCookieManager(ApplicationProperties config) {
		this.config = config;
	}

	public String getCurrentSessionId(HttpServletRequest request, String cookieName) {
		Map<String, String> sessions = getSessions(request, cookieName);
		String currentSessionId = CookieManager.getCookie(request, cookieName);
		if (StringUtils.isNotBlank(currentSessionId) && sessions.containsValue(currentSessionId)) {
			return currentSessionId;
		}
		return sessions.values().stream().findFirst().orElse(null);
	}

	public Map<String, String> getSessions(HttpServletRequest request, String cookieName) {
		Map<String, String> sessions = new LinkedHashMap<>();
		String list = CookieManager.getCookie(request, listCookie(cookieName));
		if (StringUtils.isNotBlank(list)) {
			// Dots are cookie-safe. Commas are accepted here for migration from
			// the earlier registry format, but are never written again.
			for (String slot : list.split("[.,]")) {
				if (StringUtils.isNotBlank(slot)) {
					String sid = CookieManager.getCookie(request, slotCookie(cookieName, slot));
					if (StringUtils.isNotBlank(sid)) {
						sessions.put(slot, sid);
					}
				}
			}
		}
		String currentSessionId = CookieManager.getCookie(request, cookieName);
		if (StringUtils.isNotBlank(currentSessionId) && !sessions.containsValue(currentSessionId)) {
			sessions.put(config.getCookieName(), currentSessionId);
		}
		return sessions;
	}

	public void activateSession(HttpServletRequest request, HttpServletResponse response,
			String cookieName, String sessionId) {
		if (StringUtils.isBlank(sessionId)) {
			return;
		}
		Map<String, String> sessions = getSessions(request, cookieName);
		String slot = findSlot(sessions, sessionId);
		if (slot == null) {
			slot = newSlot(sessions);
			sessions.put(slot, sessionId);
		}
		writeRegistry(request, response, cookieName, sessions, slot);
	}

	public void removeSession(HttpServletRequest request, HttpServletResponse response,
			String cookieName, String sessionId) {
		removeSessions(request, response, cookieName, List.of(sessionId));
	}

	public void removeSessions(HttpServletRequest request, HttpServletResponse response,
			String cookieName, Collection<String> sessionIds) {
		if (sessionIds == null || sessionIds.isEmpty()) {
			return;
		}
		Map<String, String> sessions = getSessions(request, cookieName);
		String currentSessionId = CookieManager.getCookie(request, cookieName);
		String currentSlot = findSlot(sessions, currentSessionId);
		List<String> removedSlots = new ArrayList<>();
		for (String sessionId : sessionIds) {
			if (StringUtils.isBlank(sessionId)) {
				continue;
			}
			String slot = findSlot(sessions, sessionId);
			if (slot != null) {
				sessions.remove(slot);
				removedSlots.add(slot);
			}
		}
		for (String slot : removedSlots) {
				CookieManager.removeCookie(slotCookie(cookieName, slot), request, response);
		}
		if (removedSlots.isEmpty()) {
			return;
		}
		String next = sessions.keySet().stream().findFirst().orElse(null);
		if (next == null) {
			CookieManager.removeCookie(listCookie(cookieName), request, response);
			CookieManager.removeCookie(cookieName, request, response);
		} else {
			writeRegistry(request, response, cookieName, sessions,
				removedSlots.contains(currentSlot) || currentSlot == null ? next : currentSlot);
		}
	}

	private void writeRegistry(HttpServletRequest request, HttpServletResponse response,
			String cookieName, Map<String, String> sessions, String currentSlot) {
		List<String> slots = new ArrayList<>(sessions.keySet());
		CookieManager.addCookie(listCookie(cookieName), String.join(".", slots), request, response, true, SESSION_MAX_AGE);
		for (Map.Entry<String, String> entry : sessions.entrySet()) {
			CookieManager.addCookie(slotCookie(cookieName, entry.getKey()), entry.getValue(), request, response, true, SESSION_MAX_AGE);
		}
		if (StringUtils.isNotBlank(currentSlot) && sessions.containsKey(currentSlot)) {
			CookieManager.addCookie(cookieName, sessions.get(currentSlot), request, response, true, SESSION_MAX_AGE);
		}
	}

	private String listCookie(String cookieName) {
		return slotCookie(cookieName, "list");
	}

	private String slotCookie(String cookieName, String slot) {
		return cookieName + SLOT_PREFIX_SEPARATOR + slot;
	}

	private String findSlot(Map<String, String> sessions, String sessionId) {
		for (Map.Entry<String, String> entry : sessions.entrySet()) {
			if (StringUtils.equals(sessionId, entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}

	private String newSlot(Map<String, String> sessions) {
		String slot;
		do {
			StringBuilder value = new StringBuilder(SLOT_LENGTH);
			for (int i = 0; i < SLOT_LENGTH; i++) {
				value.append(SLOT_ALPHABET.charAt(random.nextInt(SLOT_ALPHABET.length())));
			}
			slot = value.toString();
		} while (sessions.containsKey(slot));
		return slot;
	}
}
