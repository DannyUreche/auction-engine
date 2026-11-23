package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AdminAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AdminAccess adminAccess;

    // Profilul utilizatorului logat (inregistrarea se face prin POST /api/auth/register).
    // "admin" spune interfetei daca sa afiseze pagina de administrare.
    @GetMapping("/me")
    public CurrentUser getCurrentUser(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        return new CurrentUser(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt(),
                adminAccess.isAdmin(user.getUsername()));
    }

    public record CurrentUser(Integer id, String username, String email, LocalDateTime createdAt, boolean admin) {
    }
}
