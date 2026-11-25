package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// "Sell with AI": Claude looks at the photo of an item and suggests the name, description, category and a
// starting price for the listing. Enabled only when ANTHROPIC_API_KEY is set (app.ai.anthropic-api-key).
// The seller always reviews the suggestion in the form before publishing.
@Slf4j
@Service
public class AiListingService {

    static final String MODEL = "claude-opus-5-5";
    static final int MAX_REQUESTS_PER_HOUR = 10;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final ImageStorageService imageStorageService;
    private final AnthropicClient client;
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    public AiListingService(ImageStorageService imageStorageService,
                            @Value("${app.ai.anthropic-api-key:}") String apiKey) {
        this.imageStorageService = imageStorageService;
        this.client = apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(90))
                .build();
    }

    // What Claude returns (the JSON schema is derived from this record by the SDK)
    public record Suggestion(
            @JsonPropertyDescription("Titlul anuntului, scurt si concret (marca, model, an daca se vad), maxim 80 de caractere")
            String name,
            @JsonPropertyDescription("Descriere de 2-4 propozitii: ce este, starea vizibila in poza, detalii utile cumparatorului. Fara lucruri care nu se vad sau nu se pot deduce.")
            String description,
            @JsonPropertyDescription("Codul categoriei, exact unul dintre: ELECTRONICE, CEASURI_BIJUTERII, ARTA_COLECTII, MODA, CASA_GRADINA, SPORT, CARTI_MUZICA, VEHICULE, ALTELE")
            String category,
            @JsonPropertyDescription("Pret de pornire recomandat in lei (RON), numar intreg, prudent fata de valoarea estimata la mana a doua in Romania")
            long startPrice) {
    }

    // The answer sent to the browser: the category is checked against the enum
    public record ListingSuggestion(String name, String description, Category category, BigDecimal startPrice) {
    }

    public boolean isEnabled() {
        return client != null;
    }

    public ListingSuggestion suggest(MultipartFile file, String username) {
        if (!isEnabled()) {
            throw new IllegalArgumentException("Asistentul AI nu este activat pe acest server.");
        }
        byte[] data = read(file);
        String contentType = imageStorageService.contentTypeOf(data)
                .filter(type -> !type.equals("image/gif"))
                .orElseThrow(() -> new IllegalArgumentException("Pentru asistentul AI foloseste o poza JPG, PNG sau WebP!"));
        checkRateLimit(username);

        StructuredMessageCreateParams<Suggestion> params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(4000L)
                .system("Esti asistentul unei platforme romanesti de licitatii online (Licitatii.live). Primesti poza unui "
                        + "obiect pe care un utilizator vrea sa-l vanda si completezi anuntul. Scrie in limba romana, dar FARA "
                        + "diacritice (scrie a, i, s, t simple). Descrie doar ce se vede sau se poate deduce sigur din poza; "
                        + "daca nu recunosti modelul exact, foloseste o denumire generala. Daca poza nu arata un obiect de "
                        + "vanzare, foloseste categoria ALTELE si spune pe scurt in descriere ce se vede.")
                .outputConfig(Suggestion.class)
                .addUserMessageOfContentBlockParams(List.of(
                        ContentBlockParam.ofImage(ImageBlockParam.builder()
                                .source(Base64ImageSource.builder()
                                        .mediaType(Base64ImageSource.MediaType.of(contentType))
                                        .data(Base64.getEncoder().encodeToString(data))
                                        .build())
                                .build()),
                        ContentBlockParam.ofText(TextBlockParam.builder()
                                .text("Completeaza anuntul pentru obiectul din poza.")
                                .build())))
                .build();

        try {
            StructuredMessage<Suggestion> response = client.messages().create(params);
            if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
                throw new IllegalArgumentException("Asistentul AI nu poate analiza aceasta poza. Completeaza anuntul manual.");
            }
            Suggestion suggestion = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(typed -> typed.text())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Asistentul AI nu a trimis un raspuns. Incearca din nou."));
            return toListing(suggestion);
        } catch (RateLimitException e) {
            throw new IllegalArgumentException("Asistentul AI este ocupat acum. Incearca din nou peste un minut.");
        } catch (AnthropicServiceException e) {
            log.warn("Claude request failed: {}", e.getMessage());
            throw new IllegalArgumentException("Asistentul AI nu raspunde momentan. Completeaza anuntul manual sau incearca mai tarziu.");
        }
    }

    static ListingSuggestion toListing(Suggestion suggestion) {
        Category category = Arrays.stream(Category.values())
                .filter(value -> value.name().equalsIgnoreCase(String.valueOf(suggestion.category()).trim()))
                .findFirst()
                .orElse(Category.ALTELE);
        String name = trim(suggestion.name(), 100);
        String description = trim(suggestion.description(), 2000);
        long price = Math.max(1, suggestion.startPrice());
        return new ListingSuggestion(name, description, category, BigDecimal.valueOf(price));
    }

    private static String trim(String text, int max) {
        String value = text == null ? "" : text.trim();
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Alege o poza!");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Poza poate avea maxim 5 MB!");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Each call costs money: at most MAX_REQUESTS_PER_HOUR per user (in memory, per instance)
    private void checkRateLimit(String username) {
        long now = System.currentTimeMillis();
        Deque<Long> times = requests.computeIfAbsent(username, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > 3_600_000) {
                times.pollFirst();
            }
            if (times.size() >= MAX_REQUESTS_PER_HOUR) {
                throw new IllegalArgumentException("Ai folosit asistentul AI de " + MAX_REQUESTS_PER_HOUR
                        + " ori in ultima ora. Incearca mai tarziu.");
            }
            times.addLast(now);
        }
    }

    // Used only to keep the category list in the prompt in sync with the enum (checked by a test)
    static String categoryCodes() {
        return Arrays.stream(Category.values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
