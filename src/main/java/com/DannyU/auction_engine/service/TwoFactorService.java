package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.TwoFactorBackupCode;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.TwoFactorBackupCodeRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Two-factor authentication with an authenticator app (TOTP) and 8 one-time backup codes.
// Setup: start -> scan the QR code -> confirm with a code (only then is 2FA on). Turning it off needs the password
// and a current code. Login checks happen in AuthService.
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    static final String ISSUER = "Licitatii.live";
    private static final String BACKUP_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TwoFactorBackupCodeRepository backupCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public record Status(boolean enabled, long backupCodesLeft) {
    }

    public record Setup(String secret, String otpauthUri) {
    }

    public record Enabled(List<String> backupCodes) {
    }

    public Status status(String username) {
        User user = find(username);
        return new Status(user.isTwoFactorEnabled(), user.isTwoFactorEnabled() ? backupCodeRepository.countByUser_IdAndUsedFalse(user.getId()) : 0);
    }

    // A new secret (not active yet): the frontend shows it as a QR code
    @Transactional
    public Setup start(String username) {
        User user = find(username);
        if (user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("Autentificarea in doi pasi este deja activa.");
        }
        String secret = Totp.newSecret();
        user.setTwoFactorSecret(secret);
        userRepository.save(user);
        return new Setup(secret, Totp.uri(ISSUER, user.getUsername(), secret));
    }

    // The first correct code turns 2FA on and returns the backup codes (shown only this once)
    @Transactional
    public Enabled confirm(String username, String code) {
        User user = find(username);
        if (user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("Autentificarea in doi pasi este deja activa.");
        }
        if (user.getTwoFactorSecret() == null || !Totp.verify(user.getTwoFactorSecret(), clean(code), now())) {
            throw new IllegalArgumentException("Codul nu este corect. Verifica ora telefonului si incearca din nou.");
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        return new Enabled(newBackupCodes(user));
    }

    @Transactional
    public void disable(String username, String password, String code) {
        User user = find(username);
        if (!user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("Autentificarea in doi pasi nu este activa.");
        }
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Parola nu este corecta!");
        }
        if (!checkCode(user, code)) {
            throw new IllegalArgumentException("Codul nu este corect!");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        backupCodeRepository.deleteForUser(user.getId());
    }

    // A code from the app, or an unused backup code (which is then used up)
    @Transactional
    public boolean checkCode(User user, String code) {
        String value = clean(code);
        if (value.matches("\\d{6}")) {
            return Totp.verify(user.getTwoFactorSecret(), value, now());
        }
        return backupCodeRepository.findFirstByUser_IdAndCodeHashAndUsedFalse(user.getId(), PasswordResetService.hash(value))
                .map(backup -> {
                    backup.setUsed(true);
                    backupCodeRepository.save(backup);
                    return true;
                })
                .orElse(false);
    }

    private List<String> newBackupCodes(User user) {
        backupCodeRepository.deleteForUser(user.getId());
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            StringBuilder code = new StringBuilder();
            for (int c = 0; c < 10; c++) {
                code.append(BACKUP_ALPHABET.charAt(RANDOM.nextInt(BACKUP_ALPHABET.length())));
            }
            String text = code.substring(0, 5) + "-" + code.substring(5);
            TwoFactorBackupCode backup = new TwoFactorBackupCode();
            backup.setUser(user);
            backup.setCodeHash(PasswordResetService.hash(text));
            backupCodeRepository.save(backup);
            codes.add(text);
        }
        return codes;
    }

    private static String clean(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private static long now() {
        return System.currentTimeMillis() / 1000;
    }

    private User find(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
    }
}
