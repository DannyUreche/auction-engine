package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.dto.AuthResponse;
import com.DannyU.auction_engine.dto.LoginRequest;
import com.DannyU.auction_engine.dto.RegisterRequest;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final TwoFactorService twoFactorService;

    public record RegisterResponse(String mesaj, boolean verificationRequired) {
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Acest username este deja folosit!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Acest email este deja inregistrat!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // With email configured, the account works only after the confirmation link is opened
        user.setEmailVerified(!emailService.isConfigured());

        userRepository.save(user);
        if (!user.isEmailVerified()) {
            emailVerificationService.sendLink(user);
            return new RegisterResponse("Cont creat! Ti-am trimis un email ca sa confirmi adresa.", true);
        }
        return new RegisterResponse("Cont creat cu succes pentru utilizatorul: " + user.getUsername(), false);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Username sau parola incorecta!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Username sau parola incorecta!");
        }
        checkCanLogIn(user);

        if (user.isTwoFactorEnabled()) {
            return new AuthResponse(null, true, jwtService.generateTwoFactorTicket(user.getUsername()));
        }
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token);
    }

    // Second login step: the ticket from the password step plus a code from the app (or a backup code)
    public AuthResponse loginSecondStep(String ticket, String code) {
        User user = userRepository.findByUsername(jwtService.usernameFromTwoFactorTicket(ticket))
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        checkCanLogIn(user);
        if (!user.isTwoFactorEnabled() || !twoFactorService.checkCode(user, code)) {
            throw new IllegalArgumentException("Codul nu este corect!");
        }
        return new AuthResponse(jwtService.generateToken(user.getUsername()));
    }

    // "Sign in with Google": an existing account with the same email logs in, otherwise one is created
    public AuthResponse loginWithGoogle(String credential) {
        if (!googleTokenVerifier.isEnabled()) {
            throw new IllegalArgumentException("Autentificarea cu Google nu este configurata!");
        }
        GoogleTokenVerifier.GoogleProfile profile = googleTokenVerifier.verify(credential);
        User user = userRepository.findByEmail(profile.email()).orElseGet(() -> {
            User created = new User();
            created.setUsername(uniqueUsername(profile.email()));
            created.setEmail(profile.email());
            created.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            return userRepository.save(created);
        });
        // Google has already checked the address
        user.setEmailVerified(true);
        checkCanLogIn(user);
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user.getUsername()));
    }

    private void checkCanLogIn(User user) {
        if (user.isBlocked()) {
            throw new IllegalArgumentException("Contul tau a fost blocat de un administrator!");
        }
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Confirma adresa de email inainte sa intri in cont (cauta emailul de la noi, verifica si Spam)!");
        }
    }

    // Username from the email address (letters, digits, _), with a numeric suffix if it is taken
    private String uniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (base.length() < 3) base = (base + "user").substring(0, Math.max(3, base.length()));
        base = base.length() > 40 ? base.substring(0, 40) : base;
        String candidate = base;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + (1000 + new java.security.SecureRandom().nextInt(9000));
        }
        return candidate;
    }
}
