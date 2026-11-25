package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Statistics for a seller: per item views, bids, bidders and watchers, totals, and bids per day on their items
@Service
@RequiredArgsConstructor
public class SellerStatsService {

    private final ItemRepository itemRepository;
    private final ReviewService reviewService;
    private final EntityManager entityManager;

    public record ItemStats(Integer id, String name, String imageUrl, boolean active, boolean sold, BigDecimal currentPrice,
                            LocalDateTime endTime, int views, long bids, long bidders, long watchers) {
    }

    public record Totals(int items, int active, int sold, long views, long bids, long watchers, BigDecimal revenue,
                         ReviewService.Rating rating) {
    }

    public record DayCount(LocalDate day, long bids) {
    }

    public record SellerStats(Totals totals, List<DayCount> bidsPerDay, List<ItemStats> items) {
    }

    @Transactional(readOnly = true)
    public SellerStats statsFor(String username) {
        List<Item> items = itemRepository.findBySellerUsernameOrderByIdDesc(username);
        Map<Integer, long[]> bids = new HashMap<>();
        for (Object[] row : rows("SELECT b.item.id, COUNT(b), COUNT(DISTINCT b.bidder) FROM Bid b "
                + "WHERE b.item.seller.username = :username GROUP BY b.item.id", username)) {
            bids.put((Integer) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        Map<Integer, Long> watchers = new HashMap<>();
        for (Object[] row : rows("SELECT w.item.id, COUNT(w) FROM WatchedItem w "
                + "WHERE w.item.seller.username = :username GROUP BY w.item.id", username)) {
            watchers.put((Integer) row[0], ((Number) row[1]).longValue());
        }

        List<ItemStats> perItem = items.stream().map(item -> {
            long[] counts = bids.getOrDefault(item.getId(), new long[]{0, 0});
            return new ItemStats(item.getId(), item.getName(), item.getImageUrl(), Boolean.TRUE.equals(item.getActive()),
                    item.getWinner() != null, item.getCurrentPrice(), item.getEndTime(), item.getViewCount(),
                    counts[0], counts[1], watchers.getOrDefault(item.getId(), 0L));
        }).toList();

        BigDecimal revenue = perItem.stream().filter(ItemStats::sold).map(ItemStats::currentPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Totals totals = new Totals(perItem.size(),
                (int) perItem.stream().filter(ItemStats::active).count(),
                (int) perItem.stream().filter(ItemStats::sold).count(),
                perItem.stream().mapToLong(ItemStats::views).sum(),
                perItem.stream().mapToLong(ItemStats::bids).sum(),
                perItem.stream().mapToLong(ItemStats::watchers).sum(),
                revenue,
                reviewService.ratingOf(username));
        return new SellerStats(totals, bidsPerDay(username), perItem);
    }

    // The last 14 days, including days without bids
    private List<DayCount> bidsPerDay(String username) {
        LocalDate today = LocalDate.now();
        LocalDate first = today.minusDays(13);
        Map<LocalDate, Long> perDay = new HashMap<>();
        List<?> result = entityManager.createNativeQuery("""
                        SELECT CAST(b.created_at AS DATE) AS day, COUNT(*) FROM bids b
                        JOIN items i ON i.id = b.item_id JOIN users u ON u.id = i.seller_id
                        WHERE u.username = :username AND b.created_at >= :since
                        GROUP BY day""")
                .setParameter("username", username)
                .setParameter("since", first.atStartOfDay())
                .getResultList();
        for (Object entry : result) {
            Object[] row = (Object[]) entry;
            LocalDate day = row[0] instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) row[0];
            perDay.put(day, ((Number) row[1]).longValue());
        }
        return first.datesUntil(today.plusDays(1)).map(day -> new DayCount(day, perDay.getOrDefault(day, 0L))).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String jpql, String username) {
        return entityManager.createQuery(jpql).setParameter("username", username).getResultList();
    }
}
