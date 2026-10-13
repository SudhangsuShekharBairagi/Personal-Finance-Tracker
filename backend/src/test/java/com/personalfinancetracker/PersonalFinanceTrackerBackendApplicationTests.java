package com.personalfinancetracker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.security.AppUserPrincipal;
import com.personalfinancetracker.service.MailService;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:finance-auth-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"app.cookie-secure=false",
		"spring.mail.host=",
		"app.mail.from="
})
@AutoConfigureMockMvc
@Transactional
class PersonalFinanceTrackerBackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private SessionRegistry sessionRegistry;

	@Autowired
	private java.util.List<SecurityFilterChain> securityFilterChains;

	@MockitoBean
	private MailService mailService;

	@Test
	void registerVerifyLoginResetLogoutLifecycleUsesSessionAndSafeResponses() throws Exception {
		Cookie csrfCookie = csrfCookie();
		String email = "demo.user@example.test";
		String password = "Demo-password-123";

		mockMvc.perform(postJson("/api/auth/register", csrfCookie,
				"{\"name\":\"  Demo User  \",\"email\":\"DEMO.USER@example.test\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isAccepted());

		var registeredUser = userRepository.findByEmail(email).orElseThrow();
		assertNotNull(registeredUser.getId());
		org.junit.jupiter.api.Assertions.assertFalse(registeredUser.isEmailVerified());
		org.junit.jupiter.api.Assertions.assertEquals("Demo User", registeredUser.getName());
		org.junit.jupiter.api.Assertions.assertNotEquals(password, registeredUser.getPasswordHash());

		org.mockito.ArgumentCaptor<String> verificationCode = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(mailService).sendVerificationCode(eq(email), eq("Demo User"), verificationCode.capture(), eq(10));
		String code = verificationCode.getValue();
		org.junit.jupiter.api.Assertions.assertTrue(code.matches("[0-9]{6}"));

		mockMvc.perform(postJson("/api/auth/register", csrfCookie,
				"{\"name\":\"Other User\",\"email\":\"demo.user@example.test\",\"password\":\"Other-password-123\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REGISTRATION_UNAVAILABLE"));

		mockMvc.perform(postJson("/api/auth/login", csrfCookie,
				"{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

		mockMvc.perform(postJson("/api/auth/verify-email", csrfCookie,
				"{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		mockMvc.perform(postJson("/api/auth/verify-email", csrfCookie,
				"{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(postJson("/api/auth/login", csrfCookie,
				"{\"email\":\"" + email + "\",\"password\":\"incorrect-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

		MockHttpSession preAuthenticationSession = new MockHttpSession();
		String preAuthenticationSessionId = preAuthenticationSession.getId();
		MvcResult loginResult = mockMvc.perform(postJson("/api/auth/login", csrfCookie,
				"{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
				.session(preAuthenticationSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email").value(email))
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist())
				.andReturn();
		HttpSession loginSession = loginResult.getRequest().getSession(false);
		assertNotNull(loginSession);
		assertNotEquals(preAuthenticationSessionId, loginSession.getId());
		MockHttpSession mockLoginSession = (MockHttpSession) loginSession;
		var loginSessions = sessionRegistry.getAllSessions(new AppUserPrincipal(registeredUser), true);
		assertEquals(1, loginSessions.size());
		assertEquals(mockLoginSession.getId(), loginSessions.getFirst().getSessionId());

		mockMvc.perform(get("/api/auth/me").session(mockLoginSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Demo User"));

		mockMvc.perform(postJson("/api/auth/forgot-password", csrfCookie,
				"{\"email\":\"" + email + "\"}"))
				.andExpect(status().isAccepted())
				.andExpect(
						jsonPath("$.message").value("If the account can be recovered, a reset message will be sent."));

		org.mockito.ArgumentCaptor<String> resetCode = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(mailService).sendPasswordResetCode(eq(email), eq("Demo User"), resetCode.capture(), eq(10));
		MvcResult grantResult = mockMvc.perform(postJson("/api/auth/verify-reset-code", csrfCookie,
				"{\"email\":\"" + email + "\",\"code\":\"" + resetCode.getValue() + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode grantResponse = objectMapper.readTree(grantResult.getResponse().getContentAsString());
		String resetToken = grantResponse.get("resetToken").asText();

		String resetBody = "{\"resetToken\":\"" + resetToken + "\",\"newPassword\":\"New-demo-password-123\"}";
		mockMvc.perform(postJson("/api/auth/reset-password", csrfCookie, resetBody))
				.andExpect(status().isOk());
		mockMvc.perform(postJson("/api/auth/reset-password", csrfCookie, resetBody))
				.andExpect(status().isBadRequest());
		assertTrue(sessionRegistry.getAllSessions(new AppUserPrincipal(registeredUser), true)
				.stream().anyMatch(session -> session.isExpired()));
		mockMvc.perform(get("/api/auth/me").session(mockLoginSession))
				.andExpect(status().isUnauthorized());

		MvcResult secondLogin = mockMvc.perform(postJson("/api/auth/login", csrfCookie,
				"{\"email\":\"" + email + "\",\"password\":\"New-demo-password-123\"}"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession newSession = (MockHttpSession) secondLogin.getRequest().getSession(false);
		mockMvc.perform(post("/api/auth/logout")
				.session(newSession)
				.cookie(csrfCookie)
				.header("X-XSRF-TOKEN", csrfCookie.getValue()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/auth/me").session(newSession))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void validatesRequestsAndRequiresCsrfForStateChangingRequests() throws Exception {
		assertTrue(securityFilterChains.stream()
				.flatMap(chain -> chain.getFilters().stream())
				.anyMatch(ConcurrentSessionFilter.class::isInstance));
		Cookie csrfCookie = csrfCookie();
		mockMvc.perform(postJson("/api/auth/register", csrfCookie,
				"{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.email").exists());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"demo@example.test\",\"password\":\"some-password\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		mockMvc.perform(get("/api/private"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	private Cookie csrfCookie() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(cookie);
		return cookie;
	}

	private MockHttpServletRequestBuilder postJson(String path, Cookie csrfCookie, String json) {
		return post(path)
				.cookie(csrfCookie)
				.header("X-XSRF-TOKEN", csrfCookie.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
	}

}
