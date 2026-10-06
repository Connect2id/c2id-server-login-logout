package com.nimbusds.login.web;

import com.nimbusds.login.rest.utils.SessionCookieManager;
import com.nimbusds.login.service.C2idAuthService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class LogoutControllerTest {

	private MockMvc mockMvc;

	private C2idAuthService authService;

	private SessionCookieManager sessionCookieManager;

	@BeforeEach
	void setUp() {
		authService = Mockito.mock(C2idAuthService.class);
		sessionCookieManager = Mockito.mock(SessionCookieManager.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new LogoutController(authService, sessionCookieManager)).build();
		Mockito.when(sessionCookieManager.getCurrentSessionId(Mockito.any(), Mockito.anyString()))
				.thenReturn("sid123");
	}

	@Test
	void confirmLogout_withOpLogoutFalse_displaysRpOnlyMessage() throws Exception {
		Map<String, Object> logoutResponse = new HashMap<>();
		logoutResponse.put("type", "end");
		logoutResponse.put("sub_session_closed", false);
		logoutResponse.put("post_logout_redirect_uri", "https://client.example.com/logout?state=abc");
		logoutResponse.put("frontchannel_logout_uris", List.of("https://client.example.com/fc"));
		Mockito.when(authService.logoutSessionConfirm("lsid123", false))
				.thenReturn(logoutResponse);

		mockMvc.perform(post("/logout/confirm")
						.param("logoutSid", "lsid123")
						.param("opLogout", "false")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk())
				.andExpect(view().name("logoutEnd"))
				.andExpect(model().attribute("redirectUrl", "https://client.example.com/logout?state=abc"))
				.andExpect(model().attribute("frontChannelLogoutUrls", List.of("https://client.example.com/fc")))
				.andExpect(model().attribute("sessionClosed", false));
	}

	@Test
	void confirmLogout_withOpLogoutTrue_rendersEndResponse() throws Exception {
		Map<String, Object> logoutResponse = new HashMap<>();
		logoutResponse.put("type", "end");
		logoutResponse.put("sub_session_closed", false);
		logoutResponse.put("post_logout_redirect_uri", "https://client.example.com/logout");
		logoutResponse.put("frontchannel_logout_uris", List.of("https://rp.example/fc"));
		Mockito.when(authService.logoutSessionConfirm("lsid456", true))
				.thenReturn(logoutResponse);

		mockMvc.perform(post("/logout/confirm")
						.param("logoutSid", "lsid456")
						.param("opLogout", "true")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk())
				.andExpect(view().name("logoutEnd"))
				.andExpect(model().attribute("sessionClosed", false));
	}
}
