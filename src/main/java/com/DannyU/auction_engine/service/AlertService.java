package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.SavedAlert;
import com.DannyU.auction_engine.entity.SellerFollow;
import com.DannyU.auction_engine.repository.SellerFollowRepository;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.SavedAlertRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Saved searches ("alerts"): when an auction opens (created, or a scheduled one starts) every matching alert's
// owner gets a notification (bell, push; one per user even if several of their alerts match)
@Service
@RequiredArgsConstructor
public class AlertService {

    public static final int MAX_ALERTS = 10;

    private final SavedAlertRepository savedAlertRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SellerFollowRepository sellerFollowRepository;

    public List<SavedAlert> list(String username) {
        return savedAlertRepository.findByUserUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public SavedAlert create(String username, String query, Category category, BigDecimal minPrice, BigDecimal maxPrice) {
        String text = query == null || query.isBlank() ? null : query.trim();
        if (text == null && category == null && minPrice == null && maxPrice == null) {
            throw new IllegalArgumentException("Alege cel putin un criteriu pentru alerta (text, categorie sau pret)!");
        }
        if (text != null && text.length() > 100) {
            throw new IllegalArgumentException("Textul alertei poate avea maxim 100 de caractere!");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Pretul minim nu poate fi mai mare decat pretul maxim!");
        }
        if (savedAlertRepository.countByUserUsername(username) >= MAX_ALERTS) {
            throw new IllegalArgumentException("Poti avea maxim " + MAX_ALERTS + " alerte. Sterge una ca sa adaugi alta.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        SavedAlert alert = new SavedAlert();
        alert.setUser(user);
        alert.setQuery(text);
        alert.setCategory(category);
        alert.setMinPrice(minPrice);
        alert.setMaxPrice(maxPrice);
        return savedAlertRepository.save(alert);
    }

    @Transactional
    public void delete(Integer id, String username) {
        SavedAlert alert = savedAlertRepository.findById(id)
                .filter(found -> found.getUser().getUsername().equals(username))
                .orElseThrow(() -> new IllegalArgumentException("Alerta nu a fost gasita!"));
        savedAlertRepository.delete(alert);
    }

    // Called when an auction opens for bids: first the seller's followers, then the matching saved searches
    // (one notification per user even if they follow the seller and an alert matches too)
    @Transactional
    public void auctionOpened(Item item) {
        Set<Integer> notified = new HashSet<>();
        for (SellerFollow follow : sellerFollowRepository.findBySeller_Id(item.getSeller().getId())) {
            User follower = follow.getFollower();
            if (!follower.isBlocked() && notified.add(follower.getId())) {
                notificationService.notify(follower, Notification.Type.NEW_LISTING, item,
                        item.getSeller().getUsername() + " a pus la licitatie \"" + item.getName() + "\"");
            }
        }
        for (SavedAlert alert : savedAlertRepository.findAll()) {
            User user = alert.getUser();
            if (user.getId().equals(item.getSeller().getId()) || user.isBlocked() || !alert.matches(item)
                    || !notified.add(user.getId())) {
                continue;
            }
            notificationService.notify(user, Notification.Type.ALERT, item,
                    "Alerta ta: a aparut \"" + item.getName() + "\" (" + item.getCurrentPrice().stripTrailingZeros().toPlainString() + " RON)");
        }
    }
}
