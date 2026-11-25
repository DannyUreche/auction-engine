package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AppSetting;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.AppSettingRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// "Licitatia saptamanii": one auction highlighted on the home page. An admin can choose it (stored in
// app_settings); otherwise the busiest running auction that ends within 7 days is picked automatically.
@Service
@RequiredArgsConstructor
public class FeaturedService {

    static final String SETTING = "featured_item_id";

    private final AppSettingRepository appSettingRepository;
    private final ItemRepository itemRepository;
    private final EntityManager entityManager;

    public record Bidder(String username, BigDecimal amount, long bids) {
    }

    public record Featured(Item item, boolean chosenByAdmin, long bids, long watchers, List<Bidder> leaderboard) {
    }

    @Transactional(readOnly = true)
    public Optional<Featured> current() {
        Optional<Item> chosen = appSettingRepository.findById(SETTING)
                .flatMap(setting -> itemRepository.findById(Integer.valueOf(setting.getValue())))
                .filter(item -> Boolean.TRUE.equals(item.getActive()));
        Optional<Item> item = chosen.isPresent() ? chosen : busiest();
        return item.map(found -> new Featured(found, chosen.isPresent(), count("SELECT COUNT(b) FROM Bid b WHERE b.item.id = :id", found),
                count("SELECT COUNT(w) FROM WatchedItem w WHERE w.item.id = :id", found), leaderboard(found)));
    }

    @Transactional
    public void choose(Integer itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new IllegalArgumentException("Doar o licitatie activa poate fi licitatia saptamanii!");
        }
        AppSetting setting = appSettingRepository.findById(SETTING).orElseGet(AppSetting::new);
        setting.setKey(SETTING);
        setting.setValue(String.valueOf(itemId));
        appSettingRepository.save(setting);
    }

    @Transactional
    public void clear() {
        appSettingRepository.deleteById(SETTING);
    }

    // Started, ends within a week; most watchers and bids first
    private Optional<Item> busiest() {
        LocalDateTime now = LocalDateTime.now();
        List<Item> items = entityManager.createQuery("""
                        SELECT i FROM Item i WHERE i.active = true AND i.endTime > :now AND i.endTime < :week
                        AND (i.startTime IS NULL OR i.startTime <= :now)
                        ORDER BY ((SELECT COUNT(w) FROM WatchedItem w WHERE w.item = i) * 2
                                  + (SELECT COUNT(b) FROM Bid b WHERE b.item = i)) DESC, i.currentPrice DESC""", Item.class)
                .setParameter("now", now)
                .setParameter("week", now.plusDays(7))
                .setMaxResults(1)
                .getResultList();
        return items.stream().findFirst();
    }

    // The 5 bidders with the highest offers (one row per bidder)
    private List<Bidder> leaderboard(Item item) {
        List<Object[]> rows = entityManager.createQuery("""
                        SELECT b.bidder.username, MAX(b.amount), COUNT(b) FROM Bid b WHERE b.item.id = :id
                        GROUP BY b.bidder.username ORDER BY MAX(b.amount) DESC""", Object[].class)
                .setParameter("id", item.getId())
                .setMaxResults(5)
                .getResultList();
        return rows.stream().map(row -> new Bidder((String) row[0], (BigDecimal) row[1], (Long) row[2])).toList();
    }

    private long count(String jpql, Item item) {
        return entityManager.createQuery(jpql, Long.class).setParameter("id", item.getId()).getSingleResult();
    }
}
