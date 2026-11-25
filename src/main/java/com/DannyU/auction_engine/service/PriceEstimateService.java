package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// "Cat valoreaza?": what similar items sold for. Sold items of the same category, the ones whose names share
// words with this item first; if fewer than 3 share a word, the whole category is used. Needs at least 2 sales.
@Service
@RequiredArgsConstructor
public class PriceEstimateService {

    static final int MAX_SALES = 20;
    private static final Set<String> STOP_WORDS = Set.of("pentru", "foarte", "stare", "buna", "noua", "nou", "vechi", "set", "din", "cu");

    private final ItemRepository itemRepository;
    private final EntityManager entityManager;

    public record Sale(Integer id, String name, BigDecimal price, LocalDateTime soldAt, String imageUrl) {
    }

    // basis: SIMILAR (name match) or CATEGORY; low/high = 25th and 75th percentile
    public record Estimate(String basis, int sales, BigDecimal low, BigDecimal median, BigDecimal high,
                           BigDecimal min, BigDecimal max, List<Sale> recent) {
    }

    @Transactional(readOnly = true)
    public Optional<Estimate> estimate(Integer itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        List<Item> sold = entityManager.createQuery("""
                        SELECT i FROM Item i WHERE i.category = :category AND i.active = false AND i.winner IS NOT NULL
                        AND i.id <> :id ORDER BY i.endTime DESC""", Item.class)
                .setParameter("category", item.getCategory())
                .setParameter("id", itemId)
                .setMaxResults(200)
                .getResultList();

        Set<String> words = words(item.getName());
        List<Item> similar = sold.stream()
                .filter(other -> overlap(words, words(other.getName())) > 0)
                .sorted(Comparator.comparingInt((Item other) -> overlap(words, words(other.getName()))).reversed())
                .limit(MAX_SALES)
                .toList();
        String basis = "SIMILAR";
        if (similar.size() < 3) {
            similar = sold.stream().limit(MAX_SALES).toList();
            basis = "CATEGORY";
        }
        if (similar.size() < 2) {
            return Optional.empty();
        }

        List<BigDecimal> prices = similar.stream().map(Item::getCurrentPrice).sorted().toList();
        List<Sale> recent = similar.stream()
                .sorted(Comparator.comparing(Item::getEndTime).reversed())
                .limit(5)
                .map(other -> new Sale(other.getId(), other.getName(), other.getCurrentPrice(), other.getEndTime(), other.getImageUrl()))
                .toList();
        return Optional.of(new Estimate(basis, prices.size(), percentile(prices, 0.25), percentile(prices, 0.5),
                percentile(prices, 0.75), prices.getFirst(), prices.getLast(), recent));
    }

    // Linear interpolation between the closest ranks
    public static BigDecimal percentile(List<BigDecimal> sorted, double p) {
        double position = p * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        BigDecimal low = sorted.get(lower);
        BigDecimal value = low.add(sorted.get(upper).subtract(low).multiply(BigDecimal.valueOf(position - lower)));
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    // Lower-case words of at least 4 letters, without diacritics and common filler words
    public static Set<String> words(String name) {
        String plain = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(plain.split("[^a-z0-9]+"))
                .filter(word -> word.length() >= 4 && !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());
    }

    private static int overlap(Set<String> a, Set<String> b) {
        return (int) a.stream().filter(b::contains).count();
    }
}
