package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// "Am uitat parola": link pe email, folosibil o singura data; raspuns identic pentru emailuri inexistente
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetApiTest {

    private static final String MESAJ = "Daca exista un cont cu acest email, ti-am trimis un link de resetare a parolei.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoSpyBean
    private EmailService emailService;

    @Test
    void linkulDinEmailSchimbaParolaOSinguraData() throws Exception {
        User user = createUser();
        post("/api/auth/forgot-password", "{\"email\": \"" + user.getEmail() + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesaj").value(MESAJ));

        // Token-ul il gasim in textul emailului (link-ul .../reset-password?token=...)
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq(user.getEmail()), anyString(), text.capture());
        String token = text.getValue().replaceAll("(?s).*reset-password[?]token=([A-Za-z0-9_-]+).*", "$1");

        post("/api/auth/reset-password", "{\"token\": \"" + token + "\", \"password\": \"parolaNoua1\"}")
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("parolaNoua1", user.getPassword())).isTrue();

        // A doua oara link-ul nu mai merge
        post("/api/auth/reset-password", "{\"token\": \"" + token + "\", \"password\": \"altaParola2\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Link-ul de resetare este invalid sau a expirat!"));
    }

    @Test
    void emailInexistentPrimesteAcelasiRaspunsDarNuSeTrimiteNimic() throws Exception {
        post("/api/auth/forgot-password", "{\"email\": \"nimeni_" + UUID.randomUUID() + "@test.ro\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesaj").value(MESAJ));
        verify(emailService, never()).send(anyString(), anyString(), anyString());

        post("/api/auth/reset-password", "{\"token\": \"inventat\", \"password\": \"parolaNoua1\"}")
                .andExpect(status().isBadRequest());
        post("/api/auth/reset-password", "{\"token\": \"x\", \"password\": \"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Parola trebuie sa aiba minim 6 caractere!"));
    }

    private ResultActions post(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private User createUser() {
        String unic = "resetare_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(unic);
        user.setEmail(unic + "@test.ro");
        user.setPassword(passwordEncoder.encode("parolaVeche"));
        return userRepository.save(user);
    }
}
