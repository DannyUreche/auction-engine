package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// "Piata in cifre": public market statistics built only from aggregated data (no usernames).
// A sale = a closed item with a winner; its date is the time of its last bid (a buy-now closes before endTime).
// Computed with native PostgreSQL queries and kept for one minute, since every visitor sees the same numbers.
@Service
@RequiredArgsConstructor
public class MarketStatsService {

    static final int TREND_WEEKS = 12;
    static final int HEATMAP_DAYS = 90;
    private static final long CACHE_MILLIS = 60_000;

    private final EntityManager entityManager;
    private final ItemRepository itemRepository;
    private final Clock clock = Clock.systemUTC();

    private volatile Market cached;
    private volatile long cachedAt;

    public record Totals(long soldLast30, BigDecimal valueLast30, long bidsLast30, long activeItems, BigDecimal avgBidsPerSale,
                         Integer sellThroughPercent) {
    }

    public record CategoryRow(Category category, String label, long sold, BigDecimal avgPrice, BigDecimal medianPrice,
                              BigDecimal avgBids, Integer sellThroughPercent, long active) {
    }

    // One point per week (Monday) and category; weeks without sales are present with zeros
    public record TrendPoint(LocalDate week, Category category, long sold, BigDecimal value) {
    }

    public record Contested(Integer id, String name, String imageUrl, Category category, long bids, long bidders,
                            BigDecimal currentPrice, boolean active) {
    }

    // bids[day][hour]: day 0 = Monday, Romanian time
    public record Heatmap(long[][] bids, long max, int days) {
    }

    public record Market(LocalDateTime generatedAt, Totals totals, List<CategoryRow> categories, List<TrendPoint> trend,
                         List<Contested> contested, Heatmap heatmap) {
    }

    @Transactional(readOnly = true)
    public Market market() {
        Market current = cached;
        if (current != null && System.currentTimeMillis() - cachedAt < CACHE_MILLIS) {
            return current;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Market fresh = new Market(now, totals(now), categories(now), trend(now), contested(now), heatmap(now));
        cached = fresh;
        cachedAt = System.currentTimeMillis();
        return fresh;
    }

    // Tests create data and want fresh numbers
    public void clearCache() {
        cached = null;
    }

    // Sold items with their sale time: (category, price, bid count, sold_at)
    private static final String SALES = """
            SELECT i.id, i.category, i.current_price AS price,
                   (SELECT COUNT(*) FROM bids b WHERE b.item_id = i.id) AS bid_count,
                   (SELECT MAX(b.created_at) FROM bids b WHERE b.item_id = i.id) AS sold_at
            FROM items i WHERE i.active = FALSE AND i.winner_id IS NOT NULL""";

    private Totals totals(LocalDateTime now) {
        Object[] sales = (Object[]) entityManager.createNativeQuery("SELECT COUNT(*), COALESCE(SUM(price), 0), AVG(bid_count) FROM ("
                        + SALES + ") s WHERE s.sold_at >= :since")
                .setParameter("since", now.minusDays(30)).getSingleResult();
        long bids = number(entityManager.createNativeQuery("SELECT COUNT(*) FROM bids WHERE created_at >= :since")
                .setParameter("since", now.minusDays(30)).getSingleResult()).longValue();
        long active = number(entityManager.createNativeQuery("SELECT COUNT(*) FROM items WHERE active = TRUE").getSingleResult()).longValue();
        Object[] closed = (Object[]) entityManager.createNativeQuery("""
                        SELECT COUNT(*), COUNT(winner_id) FROM items
                        WHERE active = FALSE AND end_time >= :since""")
                .setParameter("since", now.minusDays(HEATMAP_DAYS)).getSingleResult();
        return new Totals(number(sales[0]).longValue(), money(sales[1]), bids, active, decimal(sales[2], 1),
                percent(number(closed[1]).longValue(), number(closed[0]).longValue()));
    }

    @SuppressWarnings("unchecked")
    private List<CategoryRow> categories(LocalDateTime now) {
        Map<String, Object[]> sales = toMap(entityManager.createNativeQuery("""
                SELECT s.category, COUNT(*), AVG(s.price), PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY s.price), AVG(s.bid_count)
                FROM (""" + SALES + ") s GROUP BY s.category").getResultList());
        Map<String, Object[]> closed = toMap(entityManager.createNativeQuery("""
                        SELECT category, COUNT(*), COUNT(winner_id) FROM items
                        WHERE active = FALSE AND end_time >= :since GROUP BY category""")
                .setParameter("since", now.minusDays(HEATMAP_DAYS)).getResultList());
        Map<String, Object[]> active = toMap(entityManager.createNativeQuery(
                "SELECT category, COUNT(*) FROM items WHERE active = TRUE GROUP BY category").getResultList());

        List<CategoryRow> rows = new ArrayList<>();
        for (Category category : Category.values()) {
            Object[] sale = sales.get(category.name());
            Object[] close = closed.get(category.name());
            Object[] open = active.get(category.name());
            rows.add(new CategoryRow(category, category.getLabel(),
                    sale == null ? 0 : number(sale[1]).longValue(),
                    sale == null ? null : money(sale[2]),
                    sale == null ? null : money(sale[3]),
                    sale == null ? null : decimal(sale[4], 1),
                    close == null ? null : percent(number(close[2]).longValue(), number(close[1]).longValue()),
                    open == null ? 0 : number(open[1]).longValue()));
        }
        rows.sort((a, b) -> Long.compare(b.sold(), a.sold()));
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<TrendPoint> trend(LocalDateTime now) {
        LocalDate firstWeek = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(TREND_WEEKS - 1);
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT CAST(date_trunc('week', s.sold_at) AS date), s.category, COUNT(*), SUM(s.price)
                        FROM (""" + SALES + """
                        ) s WHERE s.sold_at >= :since GROUP BY 1, 2""")
                .setParameter("since", firstWeek.atStartOfDay()).getResultList();
        Map<String, Object[]> byKey = new HashMap<>();
        for (Object[] row : rows) {
            byKey.put(toLocalDate(row[0]) + "|" + row[1], row);
        }
        List<TrendPoint> points = new ArrayList<>();
        for (int week = 0; week < TREND_WEEKS; week++) {
            LocalDate monday = firstWeek.plusWeeks(week);
            for (Category category : Category.values()) {
                Object[] row = byKey.get(monday + "|" + category.name());
                points.add(row == null ? new TrendPoint(monday, category, 0, BigDecimal.ZERO)
                        : new TrendPoint(monday, category, number(row[2]).longValue(), money(row[3])));
            }
        }
        return points;
    }

    @SuppressWarnings("unchecked")
    private List<Contested> contested(LocalDateTime now) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT b.item_id, COUNT(*), COUNT(DISTINCT b.bidder_id) FROM bids b
                        WHERE b.created_at >= :since GROUP BY b.item_id ORDER BY COUNT(*) DESC, COUNT(DISTINCT b.bidder_id) DESC
                        LIMIT 6""")
                .setParameter("since", now.minusDays(30)).getResultList();
        Map<Integer, Item> items = itemRepository.findAllById(rows.stream().map(row -> number(row[0]).intValue()).toList())
                .stream().collect(Collectors.toMap(Item::getId, Function.identity()));
        List<Contested> result = new ArrayList<>();
        for (Object[] row : rows) {
            Item item = items.get(number(row[0]).intValue());
            if (item != null) {
                result.add(new Contested(item.getId(), item.getName(), item.getImageUrl(), item.getCategory(),
                        number(row[1]).longValue(), number(row[2]).longValue(), item.getCurrentPrice(), Boolean.TRUE.equals(item.getActive())));
            }
        }
        return result;
    }

    // Bids per weekday and hour, in Romanian time (timestamps are stored in UTC)
    @SuppressWarnings("unchecked")
    private Heatmap heatmap(LocalDateTime now) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT CAST(EXTRACT(ISODOW FROM t.local) AS int), CAST(EXTRACT(HOUR FROM t.local) AS int), COUNT(*)
                        FROM (SELECT (created_at AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Bucharest' AS local
                              FROM bids WHERE created_at >= :since) t
                        GROUP BY 1, 2""")
                .setParameter("since", now.minusDays(HEATMAP_DAYS)).getResultList();
        long[][] grid = new long[7][24];
        long max = 0;
        for (Object[] row : rows) {
            int day = number(row[0]).intValue() - 1;
            int hour = number(row[1]).intValue();
            grid[day][hour] = number(row[2]).longValue();
            max = Math.max(max, grid[day][hour]);
        }
        return new Heatmap(grid, max, HEATMAP_DAYS);
    }

    private static Map<String, Object[]> toMap(List<Object[]> rows) {
        Map<String, Object[]> map = new HashMap<>();
        rows.forEach(row -> map.put(String.valueOf(row[0]), row));
        return map;
    }

    private static Number number(Object value) {
        return value == null ? 0 : (Number) value;
    }

    private static BigDecimal money(Object value) {
        return decimal(value, 2);
    }

    private static BigDecimal decimal(Object value, int scale) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString()).setScale(scale, RoundingMode.HALF_UP);
    }

    private static Integer percent(long part, long whole) {
        return whole == 0 ? null : (int) Math.round(part * 100.0 / whole);
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }
}
