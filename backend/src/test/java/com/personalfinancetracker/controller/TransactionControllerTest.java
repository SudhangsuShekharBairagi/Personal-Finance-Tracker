package com.personalfinancetracker.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.service.MailService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-transaction-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @MockitoBean
    private MailService mailService;

    @Test
    void createsAndListsOnlyOwnedTransactions() throws Exception {
        MockHttpSession session = registerAndLogin("alice@example.test", "Password-123");

        mockMvc.perform(get("/api/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        String payload = "{\"description\":\"Groceries\",\"amount\":2450.50,\"type\":\"expense\",\"category\":\"Food\",\"date\":\"2026-10-13\"}";
        MvcResult createResult = mockMvc.perform(postJson("/api/transactions", payload, session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Groceries"))
                .andReturn();

        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/transactions/{id}", id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(2450.50));

        String updated = "{\"description\":\"Groceries + pantry\",\"amount\":2750.00,\"type\":\"expense\",\"category\":\"Food\",\"date\":\"2026-10-13\"}";
        mockMvc.perform(putJson("/api/transactions/{id}", id, updated, session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Groceries + pantry"));

        Cookie csrf = csrfCookie();
        mockMvc.perform(delete("/api/transactions/{id}", id)
                .session(session)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void blocksAnotherUsersAccessToTransaction() throws Exception {
        MockHttpSession alice = registerAndLogin("alice@example.test", "Password-123");
        MockHttpSession bob = registerAndLogin("bob@example.test", "Password-456");

        String payload = "{\"description\":\"Rent\",\"amount\":26000.00,\"type\":\"expense\",\"category\":\"Housing\",\"date\":\"2026-10-01\"}";
        MvcResult createResult = mockMvc.perform(postJson("/api/transactions", payload, alice))
                .andExpect(status().isCreated())
                .andReturn();

        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/transactions/{id}", id).session(bob))
                .andExpect(status().isForbidden());

        Cookie csrf = csrfCookie();
        mockMvc.perform(delete("/api/transactions/{id}", id)
                .session(bob)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession registerAndLogin(String email, String password) throws Exception {
        Cookie csrf = csrfCookie();
        AtomicReference<String> verificationCode = new AtomicReference<>();
        doAnswer(invocation -> {
            verificationCode.set(invocation.getArgument(2));
            return null;
        }).when(mailService).sendVerificationCode(eq(email), eq("Demo User"), anyString(), eq(10));

        mockMvc.perform(postJson("/api/auth/register",
                "{\"name\":\"Demo User\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}",
                csrf))
                .andExpect(status().isAccepted());

        mockMvc.perform(postJson("/api/auth/verify-email",
                "{\"email\":\"" + email + "\",\"code\":\"" + verificationCode.get() + "\"}",
                csrf))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(postJson("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}",
                csrf))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private Cookie csrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        if (cookie == null) {
            throw new IllegalStateException("CSRF cookie missing");
        }
        return cookie;
    }

    private MockHttpServletRequestBuilder postJson(String path, String body, Cookie csrf) {
        return post(path)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder postJson(String path, String body, MockHttpSession session) throws Exception {
        Cookie csrf = csrfCookie();
        return post(path)
                .session(session)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder putJson(String path, long id, String body, MockHttpSession session)
            throws Exception {
        Cookie csrf = csrfCookie();
        return put(path, id)
                .session(session)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
