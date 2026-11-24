package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.dto.AuthResponse;
import com.DannyU.auction_engine.dto.LoginRequest;
import com.DannyU.auction_engine.dto.RegisterRequest;
import com.DannyU.auction_engine.service.AuthService;
import com.DannyU.auction_engine.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final com.DannyU.auction_engine.service.EmailVerificationService emailVerificationService;
    private final com.DannyU.auction_engine.service.GoogleTokenVerifier googleTokenVerifier;

    // "Am uitat parola": trimite un link pe email (raspunsul e acelasi daca emailul exista sau nu)
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return new MessageResponse("Daca exista un cont cu acest email, ti-am trimis un link de resetare a parolei.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return new MessageResponse("Parola a fost schimbata. Acum te poti autentifica.");
    }

    public record MessageResponse(String mesaj) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Emailul este obligatoriu!")
            @Email(message = "Adresa de email nu este valida!")
            String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Link-ul de resetare este invalid sau a expirat!")
            String token,
            @NotBlank(message = "Parola este obligatorie!")
            @Size(min = 6, message = "Parola trebuie sa aiba minim 6 caractere!")
            String password) {
    }

    @PostMapping("/register")
    public AuthService.RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // ---------- Email confirmation ----------

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody TokenRequest request) {
        emailVerificationService.verify(request.token());
        return new MessageResponse("Adresa de email a fost confirmata. Acum te poti autentifica.");
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        emailVerificationService.resend(request.email());
        return new MessageResponse("Daca adresa are un cont neconfirmat, ti-am trimis un link nou.");
    }

    public record TokenRequest(@NotBlank(message = "Link-ul de confirmare este invalid sau a expirat!") String token) {
    }

    // ---------- Sign in with Google ----------

    public record GoogleConfig(String clientId) {
    }

    public record GoogleRequest(@NotBlank(message = "Autentificarea cu Google a esuat!") String credential) {
    }

    @GetMapping("/google/config")
    public GoogleConfig googleConfig() {
        return new GoogleConfig(googleTokenVerifier.clientId());
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleRequest request) {
        return authService.loginWithGoogle(request.credential());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}