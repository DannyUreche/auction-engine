package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AccountService;
import com.DannyU.auction_engine.service.AdminAccess;
import com.DannyU.auction_engine.service.BadgeService;
import com.DannyU.auction_engine.service.FollowService;
import com.DannyU.auction_engine.service.ReviewService;
import com.DannyU.auction_engine.service.SellerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Utilizatori", description = "Contul curent, profiluri publice, insigne, urmarirea vanzatorilor, export si stergere cont.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AdminAccess adminAccess;
    private final ItemRepository itemRepository;
    private final ReviewService reviewService;
    private final AccountService accountService;
    private final SellerStatsService sellerStatsService;
    private final BadgeService badgeService;
    private final FollowService followService;

    public record DeleteRequest(String confirm) {
    }

    public record Message(String mesaj) {
    }

    // Statistics for the logged-in user as a seller (views, bids, watchers per item; bids per day)
    @io.swagger.v3.oas.annotations.Operation(summary = "Statisticile mele de vanzator")
    @GetMapping("/me/seller-stats")
    public SellerStatsService.SellerStats sellerStats(Authentication authentication) {
        return sellerStatsService.statsFor(authentication.getName());
    }

    // Everything the app stores about the logged-in user, as JSON (downloaded from the account page)
    @io.swagger.v3.oas.annotations.Operation(summary = "Exporta toate datele mele (JSON)")
    @GetMapping("/me/export")
    public ResponseEntity<Map<String, Object>> export(Authentication authentication) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"datele-mele-licitatii.json\"")
                .body(accountService.export(authentication.getName()));
    }

    // Deletes (anonymizes) the logged-in account; the body must repeat the username as confirmation
    @io.swagger.v3.oas.annotations.Operation(summary = "Sterge contul (anonimizare)")
    @DeleteMapping("/me")
    public Message deleteAccount(@RequestBody DeleteRequest request, Authentication authentication) {
        accountService.delete(authentication.getName(), request.confirm());
        return new Message("Contul tau a fost sters.");
    }

    // Profilul utilizatorului logat (inregistrarea se face prin POST /api/auth/register).
    // "admin" spune interfetei daca sa afiseze pagina de administrare.
    @io.swagger.v3.oas.annotations.Operation(summary = "Contul curent")
    @GetMapping("/me")
    public CurrentUser getCurrentUser(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        return new CurrentUser(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt(),
                adminAccess.isAdmin(user.getUsername()));
    }

    // Follow / unfollow a seller (idempotent) and the current state for the logged-in user
    @io.swagger.v3.oas.annotations.Operation(summary = "Il urmaresc pe acest vanzator?")
    @GetMapping("/{username}/follow")
    public FollowService.FollowStatus followStatus(@PathVariable String username, Authentication authentication) {
        return followService.status(authentication.getName(), username);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Urmareste vanzatorul")
    @PostMapping("/{username}/follow")
    public FollowService.FollowStatus follow(@PathVariable String username, Authentication authentication) {
        return followService.follow(authentication.getName(), username);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Nu mai urmari vanzatorul")
    @DeleteMapping("/{username}/follow")
    public FollowService.FollowStatus unfollow(@PathVariable String username, Authentication authentication) {
        return followService.unfollow(authentication.getName(), username);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Vanzatorii pe care ii urmaresc")
    @GetMapping("/me/following")
    public List<FollowService.FollowedSeller> following(Authentication authentication) {
        return followService.following(authentication.getName());
    }

    // Only the badges (the item page shows the seller's next to their name). Public.
    @io.swagger.v3.oas.annotations.Operation(summary = "Insignele unui utilizator")
    @GetMapping("/{username}/badges")
    public List<BadgeService.BadgeDto> getBadges(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        return badgeService.badgesOf(user);
    }

    // Profilul public al unui utilizator (fara email): obiectele lui active si cele vandute
    @io.swagger.v3.oas.annotations.Operation(summary = "Profilul public al unui utilizator")
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
                reviewService.receivedBy(username),
                badgeService.badgesOf(user),
                followService.followers(username));
    }

    public record PublicProfile(Integer id, String username, LocalDateTime createdAt,
                                List<Item> activeItems, List<Item> soldItems,
                                ReviewService.Rating rating, List<ReviewService.ReviewDto> reviews,
                                List<BadgeService.BadgeDto> badges, long followers) {
    }

    public record CurrentUser(Integer id, String username, String email, LocalDateTime createdAt, boolean admin) {
    }
}
