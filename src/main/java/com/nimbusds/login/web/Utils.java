package com.nimbusds.login.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.login.config.ApplicationProperties;
import com.nimbusds.login.domain.AuthenticationResult;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Utility and helper methods.
 */
@Component
public class Utils {

	private static final Logger logger = LoggerFactory.getLogger(Utils.class);

	private static String appName;

	@Value("${server.servlet.application-display-name}")
	public void setAppName(String value) {
		appName = value;
	}

	public static String getAppName() {
		return appName;
	}

	public static void updateSubjectSessionClaims(String sessionId, Map<String, Object> claims, ApplicationProperties config, ObjectMapper jsonMapper) throws IOException {
		Request.Put(config.getEndpoint() + config.getSession().getResourcePath() + "/sessions/claims").
				addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getSession().getAccessToken()).
				addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").
				addHeader("SID", sessionId).
				body(new ByteArrayEntity(jsonMapper.writeValueAsBytes(claims))).
				execute().
				handleResponse((hr) -> {
					int status = hr.getStatusLine().getStatusCode();
					if (status < 200 || status >= 300) {
						throw new IOException("Subject session claims update failed: " + hr.getStatusLine());
					}
					return null;
				});
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> extractSubjectSessionClaims(AuthenticationResult result) {
		if (result == null || result.getDetails() == null) {
			return Collections.emptyMap();
		}
		Object resultObject = result.getDetails().get("result");
		if (!(resultObject instanceof Map)) {
			return Collections.emptyMap();
		}
		Object attributesObject = ((Map<String, Object>) resultObject).get("attributes");
		if (!(attributesObject instanceof Map)) {
			return Collections.emptyMap();
		}
		Map<String, Object> claims = new HashMap<>();
		for (Map.Entry<String, Object> entry : ((Map<String, Object>) attributesObject).entrySet()) {
			String name = entry.getKey();
			if ("userID".equals(name)) {
				continue;
			}
			if ("jpegPhoto".equalsIgnoreCase(name) || "picture".equalsIgnoreCase(name)) {
				String picture = normalizePicture(entry.getValue());
				if (!StringUtils.isBlank(picture) && (!claims.containsKey("picture") || "picture".equalsIgnoreCase(name))) {
					claims.put("picture", picture);
				}
			} else {
				claims.put(name, entry.getValue());
			}
		}
		return claims;
	}

	private static String normalizePicture(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof byte[]) {
			return imageDataUri((byte[]) value);
		}

		String picture = String.valueOf(value).trim();
		if (picture.startsWith("http://") || picture.startsWith("https://")) {
			return picture;
		}
		if (picture.startsWith("data:image/") && picture.contains(";base64,")) {
			return validImageDataUri(picture) ? picture : null;
		}

		String compactBase64 = picture.replaceAll("\\s+", "");
		try {
			return imageDataUri(Base64.getDecoder().decode(compactBase64));
		} catch (IllegalArgumentException e) {
			logger.warn("Ignoring an invalid avatar value from the authentication provider");
			return null;
		}
	}

	private static String imageDataUri(byte[] image) {
		String mimeType = imageMimeType(image);
		return mimeType == null ? null : "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(image);
	}

	private static boolean validImageDataUri(String value) {
		int separator = value.indexOf(",");
		if (separator < 0) {
			return false;
		}
		try {
			return imageMimeType(Base64.getDecoder().decode(value.substring(separator + 1))) != null;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static String imageMimeType(byte[] image) {
		if (image.length >= 3 && (image[0] & 0xff) == 0xff && (image[1] & 0xff) == 0xd8
				&& (image[2] & 0xff) == 0xff) {
			return "image/jpeg";
		}
		if (image.length >= 8 && (image[0] & 0xff) == 0x89 && image[1] == 'P'
				&& image[2] == 'N' && image[3] == 'G') {
			return "image/png";
		}
		if (image.length >= 6 && image[0] == 'G' && image[1] == 'I' && image[2] == 'F'
				&& image[3] == '8') {
			return "image/gif";
		}
		if (image.length >= 12 && image[0] == 'R' && image[1] == 'I' && image[2] == 'F'
				&& image[3] == 'F' && image[8] == 'W' && image[9] == 'E'
				&& image[10] == 'B' && image[11] == 'P') {
			return "image/webp";
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static String subjectSessionId(Map<String, Object> response) {
		Object subSession = response.get("sub_session");
		if (subSession instanceof Map) {
			Object sid = ((Map<String, Object>) subSession).get("sid");
			if (sid != null) {
				return String.valueOf(sid);
			}
		}
		Object sid = response.get("sub_sid");
		return sid != null ? String.valueOf(sid) : null;
	}

	@SuppressWarnings("unchecked")
	public static void addClaimsToResponseSession(Map<String, Object> response, Map<String, Object> claims) {
		if (claims.isEmpty()) {
			return;
		}
		Object subSession = response.get("sub_session");
		if (subSession instanceof Map) {
			((Map<String, Object>) subSession).put("claims", claims);
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> authPrompt(HttpServletRequest request) {
		Object value = request.getSession().getAttribute("c2idAuthPrompt");
		if (!(value instanceof Map)) {
			return null;
		}
		Map<String, Object> prompt = (Map<String, Object>) value;
		String authSessionId = request.getParameter("authSessionId");
		return authSessionId != null && authSessionId.equals(String.valueOf(prompt.get("sid"))) ? prompt : null;
	}

	public static String requiredSubject(HttpServletRequest request) {
		Map<String, Object> prompt = authPrompt(request);
		return prompt == null ? null : value(prompt.get("required_sub"));
	}

	@SuppressWarnings("unchecked")
	public static void populatePrompt(Model model, Map<String, Object> prompt, boolean newAccountMode) {
		if (prompt == null) {
			model.addAttribute("selectAccount", false);
			model.addAttribute("createAccount", false);
			return;
		}
		String requiredSubject = value(prompt.get("required_sub"));
		String loginHint = value(prompt.get("login_hint"));
		model.addAttribute("requiredSubject", requiredSubject);
		model.addAttribute("username", StringUtils.isNotBlank(requiredSubject) ? requiredSubject : loginHint);
		model.addAttribute("selectAccount", !newAccountMode && booleanValue(prompt.get("select_account")));
		model.addAttribute("createAccount", booleanValue(prompt.get("create_account")));
	}

	public static String removeSelectAccountPrompt(String query) {
		if (StringUtils.isBlank(query)) {
			return query;
		}
		List<String> retained = new ArrayList<>();
		for (String part : query.split("&")) {
			int separator = part.indexOf('=');
			String encodedName = separator >= 0 ? part.substring(0, separator) : part;
			String encodedValue = separator >= 0 ? part.substring(separator + 1) : "";
			String name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
			String value = URLDecoder.decode(encodedValue, StandardCharsets.UTF_8);
			if (!"prompt".equals(name)) {
				retained.add(part);
				continue;
			}
			List<String> promptValues = new ArrayList<>();
			for (String promptValue : value.split("\\s+")) {
				if (!"select_account".equals(promptValue)) {
					promptValues.add(promptValue);
				}
			}
			if (!promptValues.isEmpty()) {
				retained.add(URLEncoder.encode(name, StandardCharsets.UTF_8) + "="
						+ URLEncoder.encode(String.join(" ", promptValues), StandardCharsets.UTF_8));
			}
		}
		return String.join("&", retained);
	}

	public static boolean booleanValue(Object value) {
		return value instanceof Boolean ? (Boolean) value : "true".equalsIgnoreCase(String.valueOf(value));
	}

	public static String selectionError(HttpServletRequest request, Model model, String authSessionId) {
		model.addAttribute("accountSelectionError", true);
		model.addAttribute("authenticationError", false);
		model.addAttribute("actionUrl", currentUrl(request));
		model.addAttribute("cancelUrl", request.getContextPath() + "/login/cancel" + querySuffix(request));
		model.addAttribute("authSessionId", authSessionId);
		populatePrompt(model, authPrompt(request),
				Boolean.TRUE.equals(request.getSession().getAttribute("newAccountLogin")));
		return "login";
	}

	public static String currentUrl(HttpServletRequest request) {
		return request.getContextPath() + "/login" + querySuffix(request);
	}

	public static String querySuffix(HttpServletRequest request) {
		String queryString = request.getQueryString();
		return StringUtils.isBlank(queryString) ? "" : "?" + queryString;
	}

	public static String renderString(Object val) {
		if (val == null) {
			return "";
		}
		String s = String.valueOf(val).replaceAll("[\\[\\]]", "");
		return s;
	}

	private static String value(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
