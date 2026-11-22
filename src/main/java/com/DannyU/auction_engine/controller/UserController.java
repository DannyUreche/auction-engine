package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Metoda GET pentru a vedea toti utilizatorii inregistrati
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Metoda POST pentru a inregistra un utilizator nou
    @PostMapping
    public User createUser(@RequestBody UserRequest request) {

        // Regula de business: Nu putem avea doi utilizatori cu acelasi nume!
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Acest username este deja folosit!");
        }

        // Cream utilizatorul
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());

        // Il salvam in baza de date
        return userRepository.save(newUser);
    }

    // Clasa prin care primim datele de la vizitator
    @Data
    public static class UserRequest {
        private String username;
        private String email;
    }
}