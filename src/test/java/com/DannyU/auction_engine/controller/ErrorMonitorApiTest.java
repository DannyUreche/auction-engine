package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Error monitoring: unexpected exceptions become 500 {"eroare"} and are recorded; browsers report JS errors
@SpringBootTest(properties = "app.admin-usernames=" + ErrorMonitorApiTest.ADMIN)
@AutoConfigureMockMvc
@Transactional
@Import(ErrorMonitorApiTest.FailingController.class)
class ErrorMonitorApiTest {

    static final String ADMIN = "admin_test_erori";
    // Unique per run: recorded errors are committed in their own transaction and survive the test rollback
    static final String BOOM = "Boom de test " + UUID.randomUUID();

    @TestConfiguration
    @RestController
    static class FailingController {
        @GetMapping("/api/test-only/boom")
        String boom() {
            throw new IllegalStateException(BOOM);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void serverAndClientErrorsAreGroupedForAdmins() throws Exception {
        User admin = userRepository.findByUsername(ADMIN).orElseGet(() -> {
            User user = new User();
            user.setUsername(ADMIN);
            user.setEmail(ADMIN + "@test.ro");
            user.setPassword("x");
            return userRepository.save(user);
        });
        String token = "Bearer " + jwtService.generateToken(admin.getUsername());

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/test-only/boom").header("Authorization", token))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.eroare").exists());
        }
        String jsMessage = "TypeError: x is undefined " + UUID.randomUUID();
        mockMvc.perform(post("/api/client-errors").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"" + jsMessage + "\", \"stack\": \"TypeError\\n at App.jsx:10\", \"url\": \"/items/1\"}"))
                .andExpect(status().isNoContent());

        // Unknown routes keep their 404 and are not recorded
        mockMvc.perform(get("/api/nu-exista").header("Authorization", token)).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/admin/errors").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.message == 'IllegalStateException: " + BOOM + "')].occurrences", hasItem(2)))
                .andExpect(jsonPath("$[?(@.message == '" + jsMessage + "')].source", hasItem("CLIENT")));
        mockMvc.perform(get("/api/admin/errors")).andExpect(status().isUnauthorized());

        // The server error was committed in its own transaction: remove it the same way
        new org.springframework.transaction.support.TransactionTemplate(transactionManager,
                new org.springframework.transaction.support.DefaultTransactionDefinition(
                        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW))
                .executeWithoutResult(status -> jdbcTemplate.update("DELETE FROM app_errors WHERE message LIKE ?", "%" + BOOM + "%"));
    }
}
