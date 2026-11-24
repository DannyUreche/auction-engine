package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.PasswordResetToken;
import com.DannyU.auction_engine.repository.PasswordResetTokenRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

// "Am uitat parola": link trimis pe email, valabil 30 de minute, folosibil o singura data
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    static final Duration VALIDITY = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Daca emailul nu are cont nu se intampla nimic, dar raspunsul catre browser e acelasi
    // (altfel oricine ar putea afla ce adrese de email sunt inregistrate)
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.trim()).ifPresent(user -> {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            PasswordResetToken reset = new PasswordResetToken();
            reset.setUser(user);
            reset.setTokenHash(hash(token));
            reset.setExpiresAt(LocalDateTime.now().plus(VALIDITY));
            tokenRepository.save(reset);

            emailService.send(user.getEmail(), "Resetarea parolei - Licitatii.live",
                    "Salut, " + user.getUsername() + "!\n\n"
                            + "Ai cerut resetarea parolei. Deschide link-ul de mai jos (valabil 30 de minute):\n"
                            + emailService.link("/reset-password?token=" + token) + "\n\n"
                            + "Daca nu tu ai cerut asta, ignora acest email - parola ramane aceeasi.\n\n-- \nLicitatii.live");
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken reset = tokenRepository.findByTokenHash(hash(token))
                .filter(r -> !r.isUsed() && r.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Link-ul de resetare este invalid sau a expirat!"));
        reset.setUsed(true);
        reset.getUser().setPassword(passwordEncoder.encode(newPassword));
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
