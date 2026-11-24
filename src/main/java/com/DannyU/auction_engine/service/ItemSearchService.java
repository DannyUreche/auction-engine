package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AuctionType;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Search over the active auctions, done in the database: text, category, price, auction type,
// sorting and pages. Also counts the matches per category (ignoring the category filter), for the chips.
@Service
@RequiredArgsConstructor
public class ItemSearchService {

    public static final int MAX_PAGE_SIZE = 48;

    private final EntityManager entityManager;

    public record Filters(String q, Category category, BigDecimal min, BigDecimal max, String type, String sort) {
    }

    public record Page(List<Item> items, long total, int page, int size, int totalPages,
                       Map<Category, Long> categoryCounts, long allCategories) {
    }

    @Transactional(readOnly = true)
    public Page search(Filters filters, int page, int size) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        LocalDateTime now = LocalDateTime.now();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Item> query = cb.createQuery(Item.class);
        Root<Item> item = query.from(Item.class);
        query.select(item).where(predicates(cb, item, filters, now, true)).orderBy(order(cb, item, filters.sort()));
        List<Item> items = entityManager.createQuery(query)
                .setFirstResult(safePage * safeSize)
                .setMaxResults(safeSize)
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Item> counted = countQuery.from(Item.class);
        countQuery.select(cb.count(counted)).where(predicates(cb, counted, filters, now, true));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<Object[]> perCategory = cb.createQuery(Object[].class);
        Root<Item> grouped = perCategory.from(Item.class);
        perCategory.multiselect(grouped.get("category"), cb.count(grouped))
                .where(predicates(cb, grouped, filters, now, false))
                .groupBy(grouped.get("category"));
        Map<Category, Long> counts = new EnumMap<>(Category.class);
        long all = 0;
        for (Object[] row : entityManager.createQuery(perCategory).getResultList()) {
            counts.put((Category) row[0], (Long) row[1]);
            all += (Long) row[1];
        }
        int totalPages = (int) Math.ceil(total / (double) safeSize);
        return new Page(items, total, safePage, safeSize, totalPages, counts, all);
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<Item> item, Filters filters, LocalDateTime now, boolean withCategory) {
        List<Predicate> list = new ArrayList<>();
        list.add(cb.isTrue(item.get("active")));
        if (filters.q() != null && !filters.q().isBlank()) {
            String pattern = "%" + escapeLike(filters.q().trim().toLowerCase(Locale.ROOT)) + "%";
            list.add(cb.or(
                    cb.like(cb.lower(item.get("name")), pattern, '\\'),
                    cb.like(cb.lower(item.get("description")), pattern, '\\')));
        }
        if (withCategory && filters.category() != null) {
            list.add(cb.equal(item.get("category"), filters.category()));
        }
        if (filters.min() != null) {
            list.add(cb.greaterThanOrEqualTo(item.get("currentPrice"), filters.min()));
        }
        if (filters.max() != null) {
            list.add(cb.lessThanOrEqualTo(item.get("currentPrice"), filters.max()));
        }
        Predicate started = cb.or(cb.isNull(item.get("startTime")), cb.lessThanOrEqualTo(item.get("startTime"), now));
        switch (filters.type() == null ? "" : filters.type()) {
            case "buy" -> {
                list.add(started);
                list.add(cb.equal(item.get("auctionType"), AuctionType.ENGLISH));
                list.add(cb.isNotNull(item.get("buyNowPrice")));
                list.add(cb.lessThan(item.get("currentPrice"), item.get("buyNowPrice")));
            }
            case "dutch" -> list.add(cb.equal(item.get("auctionType"), AuctionType.DUTCH));
            case "today" -> {
                list.add(started);
                list.add(cb.lessThan(item.get("endTime"), now.plusHours(24)));
            }
            case "soon" -> list.add(cb.greaterThan(item.get("startTime"), now));
            default -> {
                // all types
            }
        }
        return list.toArray(Predicate[]::new);
    }

    private List<Order> order(CriteriaBuilder cb, Root<Item> item, String sort) {
        return switch (sort == null ? "" : sort) {
            case "price-asc" -> List.of(cb.asc(item.get("currentPrice")), cb.asc(item.get("id")));
            case "price-desc" -> List.of(cb.desc(item.get("currentPrice")), cb.asc(item.get("id")));
            case "newest" -> List.of(cb.desc(item.get("id")));
            default -> List.of(cb.asc(item.get("endTime")), cb.asc(item.get("id")));
        };
    }

    // % and _ typed by the user are searched literally
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
