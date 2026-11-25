package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Advice for the sell form: what similar items sold for, how prices in the category moved month by month and
// what made listings in this category sell (start price, photos, the hour with most bids, late bidding).
// Only aggregated data, so it is public; computed from sold/closed items of the category.
@Service
@RequiredArgsConstructor
public class SellerAdviceService {

    static final int HISTORY_MONTHS = 6;

    private final EntityManager entityManager;
    private final PriceEstimateService priceEstimateService;

    public record MonthPoint(LocalDate month, long sales, BigDecimal median) {
    }

    // Sell-through (closed auctions that found a winner) for two groups, in percent; null = too few auctions
    public record Comparison(Integer withPercent, long withCount, Integer withoutPercent, long withoutCount) {
    }

    public record Advice(Category category, PriceEstimateService.Estimate estimate, BigDecimal suggestedStart,
                         BigDecimal suggestedBuyNow, List<MonthPoint> history, BigDecimal avgBids,
                         Integer bestHour, Integer bestDay, Comparison photos, Comparison lowStart, Integer lastHourPercent) {
    }

    @Transactional(readOnly = true)
    public Advice advice(String name, Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Alege categoria!");
        }
        PriceEstimateService.Estimate estimate = name == null || name.isBlank() ? null
                : priceEstimateService.estimate(name, category, null).orElse(null);
        BigDecimal suggestedStart = estimate == null ? null : nice(estimate.median().multiply(BigDecimal.valueOf(0.6)));
        BigDecimal suggestedBuyNow = estimate == null ? null : nice(estimate.high().multiply(BigDecimal.valueOf(1.1)));
        int[] best = bestHourAndDay(category);
        return new Advice(category, estimate, suggestedStart, suggestedBuyNow, history(category), avgBids(category),
                best == null ? null : best[0], best == null ? null : best[1], photos(category), lowStart(category), lastHourPercent(category));
    }

    // Median sale price per month (sale date = last bid), the empty months included
    @SuppressWarnings("unchecked")
    private List<MonthPoint> history(Category category) {
        LocalDate first = LocalDate.now().withDayOfMonth(1).minusMonths(HISTORY_MONTHS - 1);
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT CAST(date_trunc('month', s.sold_at) AS date), COUNT(*),
                               PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY s.price)
                        FROM (SELECT i.current_price AS price, (SELECT MAX(b.created_at) FROM bids b WHERE b.item_id = i.id) AS sold_at
                              FROM items i WHERE i.active = FALSE AND i.winner_id IS NOT NULL AND i.category = :category) s
                        WHERE s.sold_at >= :since GROUP BY 1""")
                .setParameter("category", category.name())
                .setParameter("since", first.atStartOfDay())
                .getResultList();
        Map<LocalDate, Object[]> byMonth = new HashMap<>();
        rows.forEach(row -> byMonth.put(LocalDate.parse(row[0].toString().substring(0, 10)), row));
        List<MonthPoint> points = new ArrayList<>();
        for (int i = 0; i < HISTORY_MONTHS; i++) {
            LocalDate month = first.plusMonths(i);
            Object[] row = byMonth.get(month);
            points.add(row == null ? new MonthPoint(month, 0, null)
                    : new MonthPoint(month, ((Number) row[1]).longValue(), decimal(row[2], 0)));
        }
        return points;
    }

    private BigDecimal avgBids(Category category) {
        Object value = entityManager.createNativeQuery("""
                        SELECT AVG((SELECT COUNT(*) FROM bids b WHERE b.item_id = i.id)) FROM items i
                        WHERE i.active = FALSE AND i.winner_id IS NOT NULL AND i.category = :category""")
                .setParameter("category", category.name()).getSingleResult();
        return decimal(value, 1);
    }

    // The Romanian hour and ISO weekday (1 = Monday) with the most bids in this category over 90 days
    @SuppressWarnings("unchecked")
    private int[] bestHourAndDay(Category category) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT CAST(EXTRACT(HOUR FROM t.local) AS int), CAST(EXTRACT(ISODOW FROM t.local) AS int)
                        FROM (SELECT (b.created_at AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Bucharest' AS local
                              FROM bids b JOIN items i ON i.id = b.item_id
                              WHERE i.category = :category AND b.created_at >= :since) t""")
                .setParameter("category", category.name())
                .setParameter("since", LocalDateTime.now().minusDays(90))
                .getResultList();
        if (rows.size() < 5) {
            return null;
        }
        int[] hours = new int[24];
        int[] days = new int[8];
        for (Object[] row : rows) {
            hours[((Number) row[0]).intValue()]++;
            days[((Number) row[1]).intValue()]++;
        }
        return new int[]{argMax(hours, 0), argMax(days, 1)};
    }

    // Sell-through of listings with 3+ photos vs fewer
    private Comparison photos(Category category) {
        return compare(category, "(SELECT COUNT(*) FROM item_photos p WHERE p.item_id = i.id) >= 3");
    }

    // Sell-through of listings that started below the category's median sale price vs at/above it
    private Comparison lowStart(Category category) {
        return compare(category, """
                i.start_price < (SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY s.current_price) FROM items s
                                 WHERE s.active = FALSE AND s.winner_id IS NOT NULL AND s.category = :category)""");
    }

    private Comparison compare(Category category, String condition) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT COUNT(*) FILTER (WHERE %1$s), COUNT(*) FILTER (WHERE %1$s AND i.winner_id IS NOT NULL),
                               COUNT(*) FILTER (WHERE NOT (%1$s)), COUNT(*) FILTER (WHERE NOT (%1$s) AND i.winner_id IS NOT NULL)
                        FROM items i WHERE i.active = FALSE AND i.category = :category""".formatted(condition))
                .setParameter("category", category.name()).getSingleResult();
        long with = ((Number) row[0]).longValue();
        long without = ((Number) row[2]).longValue();
        return new Comparison(percent(((Number) row[1]).longValue(), with), with, percent(((Number) row[3]).longValue(), without), without);
    }

    // Share of the bids on finished auctions that came in their last hour (most bidding happens at the end)
    private Integer lastHourPercent(Category category) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT COUNT(*), COUNT(*) FILTER (WHERE b.created_at >= i.end_time - INTERVAL '1 hour')
                        FROM bids b JOIN items i ON i.id = b.item_id
                        WHERE i.active = FALSE AND i.category = :category""")
                .setParameter("category", category.name()).getSingleResult();
        long all = ((Number) row[0]).longValue();
        return all < 10 ? null : (int) Math.round(((Number) row[1]).longValue() * 100.0 / all);
    }

    // A round, friendly price: 1 / 5 / 10 / 50 steps depending on size
    static BigDecimal nice(BigDecimal value) {
        double v = value.doubleValue();
        double step = v < 100 ? 5 : v < 1000 ? 10 : v < 10000 ? 50 : 100;
        return BigDecimal.valueOf(Math.max(step, Math.round(v / step) * step)).setScale(0, RoundingMode.HALF_UP);
    }

    private static int argMax(int[] values, int from) {
        int best = from;
        for (int i = from; i < values.length; i++) {
            if (values[i] > values[best]) {
                best = i;
            }
        }
        return best;
    }

    // Under 3 auctions the percentage says nothing
    private static Integer percent(long part, long whole) {
        return whole < 3 ? null : (int) Math.round(part * 100.0 / whole);
    }

    private static BigDecimal decimal(Object value, int scale) {
        return value == null ? null : new BigDecimal(value.toString()).setScale(scale, RoundingMode.HALF_UP);
    }
}
