package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.EmailVerificationToken;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.EmailVerificationTokenRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

// Email confirmation for new accounts: a single-use link, valid 48 hours
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration VALIDITY = Duration.ofHours(48);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void sendLink(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        EmailVerificationToken verification = new EmailVerificationToken();
        verification.setUser(user);
        verification.setTokenHash(PasswordResetService.hash(token));
        verification.setExpiresAt(LocalDateTime.now().plus(VALIDITY));
        tokenRepository.save(verification);

        emailService.send(user.getEmail(), "Confirma adresa de email - Licitatii.live",
                "Salut, " + user.getUsername() + "!\n\n"
                        + "Confirma adresa de email ca sa-ti activezi contul (link valabil 48 de ore):\n"
                        + emailService.link("/verify-email?token=" + token) + "\n\n-- \nLicitatii.live");
    }

    @Transactional
    public void verify(String token) {
        EmailVerificationToken verification = tokenRepository.findByTokenHash(PasswordResetService.hash(token))
                .filter(v -> !v.isUsed() && v.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Link-ul de confirmare este invalid sau a expirat!"));
        verification.setUsed(true);
        verification.getUser().setEmailVerified(true);
    }

    // Same answer whether the address exists or not
    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(email.trim()).filter(user -> !user.isEmailVerified()).ifPresent(this::sendLink);
    }
}
