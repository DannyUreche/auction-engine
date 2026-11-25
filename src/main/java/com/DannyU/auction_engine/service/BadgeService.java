package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Reputation badges, computed from the user's activity (nothing is stored). Shown on the public profile and,
// for the seller, next to their name on the item page.
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final EntityManager entityManager;
    private final ReviewService reviewService;
    private final LeaderboardService leaderboardService;

    public enum Badge {
        TRUSTED_SELLER("Vanzator de incredere", "Cel putin 5 vanzari si nota medie de minim 4,5"),
        ACTIVE_SELLER("Vanzator activ", "A pus la licitatie cel putin 10 obiecte"),
        COLLECTOR("Colectionar", "A castigat cel putin 3 licitatii"),
        PASSIONATE_BIDDER("Licitator pasionat", "Cel putin 50 de oferte plasate"),
        FIRST_BIDDER("Primul ofertant", "A deschis licitarea la cel putin 5 obiecte"),
        VETERAN("Membru veteran", "Are cont de cel putin un an"),
        TOP_BIDDER("Top licitator al lunii", "Intre primii 3 licitatori in clasamentul lunii"),
        CHALLENGER("Campionul provocarilor", "A completat toate provocarile lunii");

        private final String label;
        private final String description;

        Badge(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    public record BadgeDto(String code, String label, String description) {
    }

    @Transactional(readOnly = true)
    public List<BadgeDto> badgesOf(User user) {
        Integer id = user.getId();
        long sold = count("SELECT COUNT(i) FROM Item i WHERE i.seller.id = :id AND i.winner IS NOT NULL", id);
        long listed = count("SELECT COUNT(i) FROM Item i WHERE i.seller.id = :id", id);
        long won = count("SELECT COUNT(i) FROM Item i WHERE i.winner.id = :id AND i.active = false", id);
        long bids = count("SELECT COUNT(b) FROM Bid b WHERE b.bidder.id = :id", id);
        long firstBids = ((Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM bids b WHERE b.bidder_id = :id
                        AND b.created_at = (SELECT MIN(b2.created_at) FROM bids b2 WHERE b2.item_id = b.item_id)""")
                .setParameter("id", id).getSingleResult()).longValue();
        ReviewService.Rating rating = reviewService.ratingOf(user.getUsername());

        List<BadgeDto> badges = new ArrayList<>();
        add(badges, Badge.TRUSTED_SELLER, sold >= 5 && rating.count() > 0 && rating.average() >= 4.5);
        add(badges, Badge.ACTIVE_SELLER, listed >= 10);
        add(badges, Badge.COLLECTOR, won >= 3);
        add(badges, Badge.PASSIONATE_BIDDER, bids >= 50);
        add(badges, Badge.FIRST_BIDDER, firstBids >= 5);
        LeaderboardService.MyMonth month = leaderboardService.myMonth(user.getUsername());
        add(badges, Badge.TOP_BIDDER, month.bidderRank() != null && month.bidderRank() <= 3);
        add(badges, Badge.CHALLENGER, month.completed() == month.challenges().size());
        add(badges, Badge.VETERAN, user.getCreatedAt() != null && user.getCreatedAt().isBefore(LocalDateTime.now().minusYears(1)));
        return badges;
    }

    private static void add(List<BadgeDto> badges, Badge badge, boolean earned) {
        if (earned) {
            badges.add(new BadgeDto(badge.name(), badge.label, badge.description));
        }
    }

    private long count(String jpql, Integer id) {
        return entityManager.createQuery(jpql, Long.class).setParameter("id", id).getSingleResult();
    }
}
