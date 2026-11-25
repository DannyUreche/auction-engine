package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import com.DannyU.auction_engine.service.Totp;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Two-factor authentication: setup, two-step login, backup codes, the ticket is not a login token
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TwoFactorApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void totpMatchesTheRfcTestVector() {
        // RFC 6238 test secret "12345678901234567890" (Base32 GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ), T = 59 s -> 94287082
        assertThat(Totp.code("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 59 / 30)).isEqualTo("287082");
    }

    @Test
    void setupAndTwoStepLogin() throws Exception {
        User user = user();
        String token = "Bearer " + jwtService.generateToken(user.getUsername());

        String setup = post("/api/users/me/2fa/setup", token, "{}").andExpect(status().isOk())
                .andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                .andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(setup, "$.secret");
        post("/api/users/me/2fa/enable", token, "{\"code\": \"000000\"}").andExpect(status().isBadRequest());
        String enabled = post("/api/users/me/2fa/enable", token, "{\"code\": \"" + now(secret) + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> backupCodes = JsonPath.read(enabled, "$.backupCodes");
        assertThat(backupCodes).hasSize(8);
        mockMvc.perform(get("/api/users/me/2fa").header("Authorization", token))
                .andExpect(jsonPath("$.enabled").value(true)).andExpect(jsonPath("$.backupCodesLeft").value(8));

        // Password step: no token yet, only a ticket - and the ticket can't be used as a token
        String first = login(user).andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.twoFactorRequired").value(true)).andReturn().getResponse().getContentAsString();
        String ticket = JsonPath.read(first, "$.twoFactorTicket");
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + ticket)).andExpect(status().isUnauthorized());

        secondStep(ticket, "123456").andExpect(status().isBadRequest());
        secondStep(ticket, now(secret)).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());

        // A backup code works once
        secondStep(ticket, backupCodes.getFirst().toUpperCase()).andExpect(status().isOk());
        secondStep(ticket, backupCodes.getFirst()).andExpect(status().isBadRequest());

        post("/api/users/me/2fa/disable", token, "{\"password\": \"gresita\", \"code\": \"" + now(secret) + "\"}")
                .andExpect(status().isBadRequest());
        post("/api/users/me/2fa/disable", token, "{\"password\": \"parola123\", \"code\": \"" + now(secret) + "\"}")
                .andExpect(status().isNoContent());
        login(user).andExpect(jsonPath("$.token").isNotEmpty());
    }

    private static String now(String secret) {
        return Totp.code(secret, System.currentTimeMillis() / 1000 / 30);
    }

    private ResultActions login(User user) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + user.getUsername() + "\", \"password\": \"parola123\"}"));
    }

    private ResultActions secondStep(String ticket, String code) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login/2fa").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticket\": \"" + ticket + "\", \"code\": \"" + code + "\"}"));
    }

    private ResultActions post(String url, String token, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private User user() {
        User user = new User();
        user.setUsername("doi_pasi_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword(passwordEncoder.encode("parola123"));
        return userRepository.save(user);
    }
}
