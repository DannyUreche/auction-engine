package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Counties;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// "Cauta cu AI": a search written in plain words becomes the normal search filters. Claude's answer is checked
// here (known category, real county, sane prices, known type/sort), so the browser only gets values the
// regular search accepts. Rate limited per user, since each call costs money.
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchService {

    static final int MAX_REQUESTS_PER_HOUR = 30;
    static final int MAX_QUERY_LENGTH = 200;
    private static final Set<String> TYPES = Set.of("buy", "dutch", "today", "soon");
    private static final Set<String> SORTS = Set.of("ending", "price-asc", "price-desc", "newest");

    private final SearchInterpreter interpreter;
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    // The filters the home page applies (names match its URL parameters)
    public record SearchFilters(String q, String cat, String county, BigDecimal min, BigDecimal max, String type,
                                String sort, String explanation) {
    }

    public boolean isEnabled() {
        return interpreter.isEnabled();
    }

    public SearchFilters interpret(String query, String username) {
        if (!isEnabled()) {
            throw new IllegalArgumentException("Cautarea cu AI nu este activata pe acest server.");
        }
        String text = query == null ? "" : query.trim();
        if (text.length() < 3) {
            throw new IllegalArgumentException("Scrie ce cauti (cel putin 3 caractere).");
        }
        if (text.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Cererea poate avea maxim " + MAX_QUERY_LENGTH + " de caractere.");
        }
        checkRateLimit(username);
        try {
            return interpreter.interpret(text)
                    .map(AiSearchService::clean)
                    .orElseThrow(() -> new IllegalArgumentException("Asistentul AI nu poate cauta asta. Incearca alte cuvinte."));
        } catch (RateLimitException e) {
            throw new IllegalArgumentException("Asistentul AI este ocupat acum. Incearca din nou peste un minut.");
        } catch (AnthropicServiceException e) {
            log.warn("Claude search request failed: {}", e.getMessage());
            throw new IllegalArgumentException("Asistentul AI nu raspunde momentan. Foloseste filtrele obisnuite.");
        }
    }

    static SearchFilters clean(SearchInterpreter.Interpretation answer) {
        String category = Arrays.stream(Category.values())
                .map(Enum::name)
                .filter(name -> name.equalsIgnoreCase(blankToEmpty(answer.category())))
                .findFirst()
                .orElse(null);
        String county;
        try {
            county = Counties.canonical(answer.county());
        } catch (IllegalArgumentException e) {
            county = null; // a town or a typo Claude didn't map: better no county than an error
        }
        BigDecimal min = answer.minPrice() > 0 ? BigDecimal.valueOf(answer.minPrice()) : null;
        BigDecimal max = answer.maxPrice() > 0 ? BigDecimal.valueOf(answer.maxPrice()) : null;
        if (min != null && max != null && min.compareTo(max) > 0) {
            BigDecimal swap = min;
            min = max;
            max = swap;
        }
        String type = TYPES.contains(blankToEmpty(answer.type())) ? answer.type().trim() : null;
        String sort = SORTS.contains(blankToEmpty(answer.sort())) ? answer.sort().trim() : null;
        String keywords = blankToEmpty(answer.keywords());
        if (keywords.length() > 60) {
            keywords = keywords.substring(0, 60);
        }
        return new SearchFilters(keywords.isEmpty() ? null : keywords, category, county, min, max, type, sort,
                blankToEmpty(answer.explanation()));
    }

    private static String blankToEmpty(String text) {
        return text == null ? "" : text.trim();
    }

    private void checkRateLimit(String username) {
        long now = System.currentTimeMillis();
        Deque<Long> times = requests.computeIfAbsent(username, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > 3_600_000) {
                times.pollFirst();
            }
            if (times.size() >= MAX_REQUESTS_PER_HOUR) {
                throw new IllegalArgumentException("Ai folosit cautarea cu AI de " + MAX_REQUESTS_PER_HOUR
                        + " ori in ultima ora. Incearca mai tarziu.");
            }
            times.addLast(now);
        }
    }
}
