package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Monthly leaderboard (most active bidders, best sellers) and the monthly challenges of a user.
// Everything is computed from the activity of the current calendar month (UTC).
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    public record BidderRow(String username, long bids, long items, BigDecimal highestBid) {
    }

    public record SellerRow(String username, long sold, BigDecimal value) {
    }

    public record Leaderboard(LocalDateTime monthStart, LocalDateTime monthEnd, List<BidderRow> bidders, List<SellerRow> sellers) {
    }

    public record Challenge(String code, String title, String description, String icon, long progress, long target) {
        public boolean done() {
            return progress >= target;
        }
    }

    public record MyMonth(List<Challenge> challenges, int completed, Integer bidderRank, Integer sellerRank) {
    }

    static LocalDateTime monthStart() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    @Transactional(readOnly = true)
    public Leaderboard leaderboard() {
        LocalDateTime start = monthStart();
        return new Leaderboard(start, start.plusMonths(1), bidders(start, 10), sellers(start, 10));
    }

    @Transactional(readOnly = true)
    public MyMonth myMonth(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        LocalDateTime start = monthStart();
        long bids = count("SELECT COUNT(b) FROM Bid b WHERE b.bidder = :user AND b.createdAt >= :start", user, start);
        long categories = count("SELECT COUNT(DISTINCT b.item.category) FROM Bid b WHERE b.bidder = :user AND b.createdAt >= :start", user, start);
        long won = count("SELECT COUNT(i) FROM Item i WHERE i.winner = :user AND i.active = false AND i.endTime >= :start", user, start);
        long watched = count("SELECT COUNT(w) FROM WatchedItem w WHERE w.user = :user AND w.createdAt >= :start", user, start);
        long questions = count("SELECT COUNT(q) FROM Question q WHERE q.author = :user AND q.createdAt >= :start", user, start);
        List<Challenge> challenges = List.of(
                new Challenge("BIDS", "Licitator activ", "Plaseaza 10 oferte luna aceasta", "gavel", Math.min(bids, 10), 10),
                new Challenge("CATEGORIES", "Explorator", "Liciteaza in 3 categorii diferite", "sparkles", Math.min(categories, 3), 3),
                new Challenge("WIN", "Castigator", "Castiga o licitatie", "trophy", Math.min(won, 1), 1),
                new Challenge("WATCH", "Vanator de chilipiruri", "Urmareste 5 obiecte noi", "heart", Math.min(watched, 5), 5),
                new Challenge("QUESTION", "Curios", "Pune o intrebare unui vanzator", "chat", Math.min(questions, 1), 1));
        int completed = (int) challenges.stream().filter(Challenge::done).count();
        return new MyMonth(challenges, completed, rank(bidders(start, 1000).stream().map(BidderRow::username).toList(), username),
                rank(sellers(start, 1000).stream().map(SellerRow::username).toList(), username));
    }

    private List<BidderRow> bidders(LocalDateTime start, int limit) {
        return entityManager.createQuery("""
                        SELECT b.bidder.username, COUNT(b), COUNT(DISTINCT b.item.id), MAX(b.amount) FROM Bid b
                        WHERE b.createdAt >= :start GROUP BY b.bidder.username ORDER BY COUNT(b) DESC, MAX(b.amount) DESC""", Object[].class)
                .setParameter("start", start).setMaxResults(limit).getResultList().stream()
                .map(row -> new BidderRow((String) row[0], (Long) row[1], (Long) row[2], (BigDecimal) row[3])).toList();
    }

    private List<SellerRow> sellers(LocalDateTime start, int limit) {
        return entityManager.createQuery("""
                        SELECT i.seller.username, COUNT(i), SUM(i.currentPrice) FROM Item i
                        WHERE i.winner IS NOT NULL AND i.active = false AND i.endTime >= :start
                        GROUP BY i.seller.username ORDER BY SUM(i.currentPrice) DESC""", Object[].class)
                .setParameter("start", start).setMaxResults(limit).getResultList().stream()
                .map(row -> new SellerRow((String) row[0], (Long) row[1], (BigDecimal) row[2])).toList();
    }

    private static Integer rank(List<String> usernames, String username) {
        int index = usernames.indexOf(username);
        return index < 0 ? null : index + 1;
    }

    private long count(String jpql, User user, LocalDateTime start) {
        return entityManager.createQuery(jpql, Long.class).setParameter("user", user).setParameter("start", start).getSingleResult();
    }
}
