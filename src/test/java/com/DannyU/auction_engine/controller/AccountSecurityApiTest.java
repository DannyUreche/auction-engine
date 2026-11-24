package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.EmailService;
import com.DannyU.auction_engine.service.GoogleTokenVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Email confirmation (with sending emails "configured") and "Sign in with Google" (Google is mocked)
@SpringBootTest(properties = "spring.mail.host=localhost")
@AutoConfigureMockMvc
@Transactional
class AccountSecurityApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @MockitoSpyBean
    private EmailService emailService;
    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @Test
    void newAccountsMustConfirmTheirEmailBeforeLoggingIn() throws Exception {
        doNothing().when(emailService).send(anyString(), anyString(), anyString());
        String username = "confirm_" + UUID.randomUUID().toString().substring(0, 8);

        post("/api/auth/register", "{\"username\": \"" + username + "\", \"email\": \"" + username + "@test.ro\", \"password\": \"parola123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationRequired").value(true));
        post("/api/auth/login", "{\"username\": \"" + username + "\", \"password\": \"parola123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value(org.hamcrest.Matchers.startsWith("Confirma adresa de email")));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq(username + "@test.ro"), anyString(), text.capture());
        String token = text.getValue().replaceAll("(?s).*verify-email[?]token=([A-Za-z0-9_-]+).*", "$1");

        post("/api/auth/verify-email", "{\"token\": \"" + token + "\"}").andExpect(status().isOk());
        post("/api/auth/login", "{\"username\": \"" + username + "\", \"password\": \"parola123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
        post("/api/auth/verify-email", "{\"token\": \"" + token + "\"}").andExpect(status().isBadRequest());
    }

    @Test
    void googleSignInCreatesOrReusesTheAccountWithTheSameEmail() throws Exception {
        String email = "google_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        when(googleTokenVerifier.isEnabled()).thenReturn(true);
        when(googleTokenVerifier.clientId()).thenReturn("client-test");
        when(googleTokenVerifier.verify("token-bun")).thenReturn(new GoogleTokenVerifier.GoogleProfile(email, "Ana"));

        mockMvc.perform(get("/api/auth/google/config")).andExpect(jsonPath("$.clientId").value("client-test"));
        post("/api/auth/google", "{\"credential\": \"token-bun\"}").andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        post("/api/auth/google", "{\"credential\": \"token-bun\"}").andExpect(status().isOk());

        assertThat(userRepository.findByEmail(email)).hasValueSatisfying(user -> {
            assertThat(user.getUsername()).startsWith("google_");
            assertThat(user.isEmailVerified()).isTrue();
        });
    }

    private ResultActions post(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
