package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AdminAccess;
import com.DannyU.auction_engine.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AdminAccess adminAccess;
    private final ItemRepository itemRepository;
    private final ReviewService reviewService;

    // Profilul utilizatorului logat (inregistrarea se face prin POST /api/auth/register).
    // "admin" spune interfetei daca sa afiseze pagina de administrare.
    @GetMapping("/me")
    public CurrentUser getCurrentUser(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        return new CurrentUser(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt(),
                adminAccess.isAdmin(user.getUsername()));
    }

    // Profilul public al unui utilizator (fara email): obiectele lui active si cele vandute
    @GetMapping("/{username}/profile")
    public PublicProfile getProfile(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        List<Item> items = itemRepository.findBySellerUsernameOrderByIdDesc(username);
        return new PublicProfile(
                user.getId(),
                user.getUsername(),
                user.getCreatedAt(),
                items.stream().filter(Item::getActive).toList(),
                items.stream().filter(item -> !item.getActive() && item.getWinner() != null).toList(),
                reviewService.ratingOf(username),
                reviewService.receivedBy(username));
    }

    public record PublicProfile(Integer id, String username, LocalDateTime createdAt,
                                List<Item> activeItems, List<Item> soldItems,
                                ReviewService.Rating rating, List<ReviewService.ReviewDto> reviews) {
    }

    public record CurrentUser(Integer id, String username, String email, LocalDateTime createdAt, boolean admin) {
    }
}
