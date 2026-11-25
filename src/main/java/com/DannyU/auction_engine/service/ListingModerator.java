package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Report;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

// Asks Claude whether a new listing breaks the rules (text + main photo thumbnail).
// The answer is only a suggestion for the admins: nothing is hidden or deleted automatically.
@Service
@RequiredArgsConstructor
public class ListingModerator {

    private final ClaudeClient claude;
    private final ThumbnailService thumbnailService;

    // What Claude returns (the SDK turns this record into the JSON schema)
    public record Verdict(
            @JsonPropertyDescription("true daca anuntul trebuie verificat de un administrator")
            boolean suspicious,
            @JsonPropertyDescription("Motivul, exact unul dintre: FRAUD, PROHIBITED, WRONG_CATEGORY, OFFENSIVE, OTHER (NONE daca nu e suspect)")
            String reason,
            @JsonPropertyDescription("O propozitie scurta pentru administrator, in romana fara diacritice")
            String explanation) {
    }

    public boolean isEnabled() {
        return claude.isEnabled();
    }

    public Verdict review(Item item) {
        List<ContentBlockParam> content = new ArrayList<>();
        mainPhoto(item).ifPresent(jpeg -> content.add(ContentBlockParam.ofImage(ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                        .data(Base64.getEncoder().encodeToString(jpeg))
                        .build())
                .build())));
        content.add(ContentBlockParam.ofText(TextBlockParam.builder().text(describe(item)).build()));

        StructuredMessageCreateParams<Verdict> params = MessageCreateParams.builder()
                .model(ClaudeClient.MODEL)
                .maxTokens(2000L)
                .system("Esti moderatorul platformei romanesti de licitatii Licitatii.live. Verifici un anunt nou (text si, "
                        + "daca exista, poza principala) si spui daca un administrator trebuie sa se uite la el. Suspecte sunt: "
                        + "obiecte interzise (arme, droguri, medicamente cu reteta, animale vii, documente oficiale, produse "
                        + "contrafacute, continut pentru adulti), semne de frauda (cere plata sau contact in afara platformei, "
                        + "numere de telefon, linkuri, pret de nerealist de mic pentru ce descrie), limbaj ofensator sau o "
                        + "categorie evident gresita. Fii prudent: un anunt obisnuit NU este suspect. Explicatia o scrii in "
                        + "romana, fara diacritice.")
                .outputConfig(Verdict.class)
                .addUserMessageOfContentBlockParams(content)
                .build();

        StructuredMessage<Verdict> response = claude.get().messages().create(params);
        // A refusal on a sales listing is itself a reason for a human to look
        if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            return new Verdict(true, "OTHER", "Asistentul AI a refuzat sa analizeze anuntul; verifica-l manual.");
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(typed -> typed.text())
                .findFirst()
                .orElse(new Verdict(false, "NONE", ""));
    }

    // Unknown reasons count as OTHER
    public static Report.Reason reasonOf(Verdict verdict) {
        return Arrays.stream(Report.Reason.values())
                .filter(reason -> reason.name().equalsIgnoreCase(String.valueOf(verdict.reason()).trim()))
                .findFirst()
                .orElse(Report.Reason.OTHER);
    }

    private Optional<byte[]> mainPhoto(Item item) {
        if (item.getImageFilenames().isEmpty()) {
            return Optional.empty();
        }
        return thumbnailService.thumbnail(item.getImageFilenames().getFirst());
    }

    private static String describe(Item item) {
        return "Titlu: " + item.getName()
                + "\nDescriere: " + (item.getDescription() == null ? "(fara)" : item.getDescription())
                + "\nCategorie: " + item.getCategory().getLabel()
                + "\nPret de pornire: " + item.getStartPrice().toPlainString() + " RON"
                + (item.getBuyNowPrice() != null ? "\nCumpara acum: " + item.getBuyNowPrice().toPlainString() + " RON" : "")
                + (item.getCounty() != null ? "\nJudet: " + item.getCounty() : "");
    }
}
