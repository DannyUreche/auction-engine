package com.DannyU.auction_engine.service;

import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

// Asks Claude to turn a search typed in plain words ("ceas vechi sub 500 lei in Cluj") into the filters of the
// home page search. Only the Claude call lives here, so tests can mock it; checks are in AiSearchService.
@Service
@RequiredArgsConstructor
public class SearchInterpreter {

    private final ClaudeClient claude;

    // What Claude returns (the SDK turns this record into the JSON schema)
    public record Interpretation(
            @JsonPropertyDescription("1-3 cuvinte cheie cautate in titlu/descriere, fara diacritice (marca, model, tipul obiectului). Gol daca cererea e doar o categorie, un pret sau un judet.")
            String keywords,
            @JsonPropertyDescription("Codul categoriei sau gol, exact unul dintre: ELECTRONICE, CEASURI_BIJUTERII, ARTA_COLECTII, MODA, CASA_GRADINA, SPORT, CARTI_MUZICA, VEHICULE, ALTELE")
            String category,
            @JsonPropertyDescription("Judetul din Romania (sau Bucuresti) daca utilizatorul cere o zona, altfel gol. Un oras se transforma in judetul lui (Cluj-Napoca -> Cluj, Iasi -> Iasi).")
            String county,
            @JsonPropertyDescription("Pretul minim in lei, 0 daca nu e cerut")
            long minPrice,
            @JsonPropertyDescription("Pretul maxim in lei, 0 daca nu e cerut (\"sub 500\" -> 500, \"pana in 1000 de euro\" -> aprox. 5000)")
            long maxPrice,
            @JsonPropertyDescription("Tipul licitatiei sau gol: buy (are Cumpara acum), dutch (licitatie olandeza, pretul scade), today (se termina azi), soon (incepe curand)")
            String type,
            @JsonPropertyDescription("Ordonarea sau gol: ending (se termina curand), price-asc (cele mai ieftine), price-desc (cele mai scumpe), newest (cele mai noi)")
            String sort,
            @JsonPropertyDescription("O propozitie scurta, in romana fara diacritice, care spune ce filtre ai ales (ex: \"Ceasuri din Cluj sub 500 lei, cele mai ieftine primele\")")
            String explanation) {
    }

    public boolean isEnabled() {
        return claude.isEnabled();
    }

    // Empty when Claude refuses
    public Optional<Interpretation> interpret(String query) {
        StructuredMessageCreateParams<Interpretation> params = MessageCreateParams.builder()
                .model(ClaudeClient.MODEL)
                .maxTokens(1500L)
                .system("Esti asistentul de cautare al platformei romanesti de licitatii Licitatii.live. Primesti ce cauta "
                        + "un vizitator, in cuvintele lui, si alegi filtrele cautarii. Pune doar filtrele care reies din "
                        + "cerere; nu inventa preturi sau judete. Cuvintele cheie trebuie sa apara in titlul unui anunt, "
                        + "deci foloseste termeni concreti si scurti (\"ceas\", \"iphone\", \"bicicleta\"), nu fraze. Daca "
                        + "cererea nu are legatura cu cumpararea unor obiecte, lasa toate filtrele goale si spune asta in "
                        + "explicatie.")
                .outputConfig(Interpretation.class)
                .addUserMessage(query)
                .build();
        StructuredMessage<Interpretation> response = claude.get().messages().create(params);
        if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            return Optional.empty();
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(typed -> typed.text())
                .findFirst();
    }
}
