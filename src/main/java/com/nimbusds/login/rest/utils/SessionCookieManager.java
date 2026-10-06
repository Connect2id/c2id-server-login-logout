package com.nimbusds.login.rest.utils;

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

	private static final String SESSION_LIST_COOKIE = "sid-list";
	private static final String CURRENT_SESSION_COOKIE = "current-session";
	private static final String SLOT_PREFIX = "sid-";
	private static final String SLOT_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int SLOT_LENGTH = 2;
	private static final int SESSION_MAX_AGE = 60 * 60 * 24 * 30;

	private final SecureRandom random = new SecureRandom();

	public String getCurrentSessionId(HttpServletRequest request, String legacyCookieName) {
		Map<String, String> sessions = getSessions(request, legacyCookieName);
		String currentSlot = CookieManager.getCookie(request, CURRENT_SESSION_COOKIE);
		if (StringUtils.isNotBlank(currentSlot) && sessions.containsKey(currentSlot)) {
			return sessions.get(currentSlot);
		}
		return sessions.values().stream().findFirst().orElse(null);
	}

	public Map<String, String> getSessions(HttpServletRequest request, String legacyCookieName) {
		Map<String, String> sessions = new LinkedHashMap<>();
		String list = CookieManager.getCookie(request, SESSION_LIST_COOKIE);
		if (StringUtils.isNotBlank(list)) {
			// Dots are cookie-safe. Commas are accepted here for migration from
			// the earlier registry format, but are never written again.
			for (String slot : list.split("[.,]")) {
				if (StringUtils.isNotBlank(slot)) {
					String sid = CookieManager.getCookie(request, SLOT_PREFIX + slot);
					if (StringUtils.isNotBlank(sid)) {
						sessions.put(slot, sid);
					}
				}
			}
		}
		String legacySid = CookieManager.getCookie(request, legacyCookieName);
		if (StringUtils.isNotBlank(legacySid) && !sessions.containsValue(legacySid)) {
			sessions.put("legacy", legacySid);
		}
		return sessions;
	}

	public void activateSession(HttpServletRequest request, HttpServletResponse response,
			String legacyCookieName, String sessionId) {
		if (StringUtils.isBlank(sessionId)) {
			return;
		}
		Map<String, String> sessions = getSessions(request, legacyCookieName);
		String slot = findSlot(sessions, sessionId);
		if (slot == null || "legacy".equals(slot)) {
			slot = newSlot(sessions);
			sessions.put(slot, sessionId);
		}
		writeRegistry(request, response, legacyCookieName, sessions, slot);
	}

	public void removeSession(HttpServletRequest request, HttpServletResponse response,
			String legacyCookieName, String sessionId) {
		removeSessions(request, response, legacyCookieName, List.of(sessionId));
	}

	public void removeSessions(HttpServletRequest request, HttpServletResponse response,
			String legacyCookieName, Collection<String> sessionIds) {
		if (sessionIds == null || sessionIds.isEmpty()) {
			return;
		}
		Map<String, String> sessions = getSessions(request, legacyCookieName);
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
			CookieManager.removeCookie(SLOT_PREFIX + slot, request, response);
		}
		if (removedSlots.isEmpty()) {
			return;
		}
		String current = CookieManager.getCookie(request, CURRENT_SESSION_COOKIE);
		String next = sessions.keySet().stream().findFirst().orElse(null);
		if (next == null) {
			CookieManager.removeCookie(SESSION_LIST_COOKIE, request, response);
			CookieManager.removeCookie(CURRENT_SESSION_COOKIE, request, response);
		} else {
			writeRegistry(request, response, legacyCookieName, sessions,
				removedSlots.contains(current) ? next : current);
		}
	}

	private void writeRegistry(HttpServletRequest request, HttpServletResponse response,
			String legacyCookieName, Map<String, String> sessions, String currentSlot) {
		List<String> slots = new ArrayList<>(sessions.keySet());
		CookieManager.addCookie(SESSION_LIST_COOKIE, String.join(".", slots), request, response, true, SESSION_MAX_AGE);
		for (Map.Entry<String, String> entry : sessions.entrySet()) {
			CookieManager.addCookie(SLOT_PREFIX + entry.getKey(), entry.getValue(), request, response, true, SESSION_MAX_AGE);
		}
		if (StringUtils.isNotBlank(currentSlot) && sessions.containsKey(currentSlot)) {
			CookieManager.addCookie(CURRENT_SESSION_COOKIE, currentSlot, request, response, true, SESSION_MAX_AGE);
		}
		if (CookieManager.getCookie(request, legacyCookieName) != null) {
			CookieManager.removeCookie(legacyCookieName, request, response);
		}
	}

	private String findSlot(Map<String, String> sessions, String sessionId) {
		for (Map.Entry<String, String> entry : sessions.entrySet()) {
			if (sessionId.equals(entry.getValue())) {
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
