package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Claude's search answer is checked before it reaches the browser (the interpreter is mocked)
class AiSearchServiceTest {

    @Test
    void answerIsTurnedIntoValidFilters() {
        var filters = AiSearchService.clean(new SearchInterpreter.Interpretation(
                " ceas ", "ceasuri_bijuterii", "cluj", 900, 100, "buy", "price-asc", "Ceasuri din Cluj"));
        assertThat(filters.q()).isEqualTo("ceas");
        assertThat(filters.cat()).isEqualTo("CEASURI_BIJUTERII");
        assertThat(filters.county()).isEqualTo("Cluj");
        assertThat(filters.min()).isEqualByComparingTo(BigDecimal.valueOf(100)); // swapped
        assertThat(filters.max()).isEqualByComparingTo(BigDecimal.valueOf(900));
        assertThat(filters.type()).isEqualTo("buy");
        assertThat(filters.sort()).isEqualTo("price-asc");
    }

    @Test
    void unknownValuesAreDropped() {
        var filters = AiSearchService.clean(new SearchInterpreter.Interpretation(
                "", "MASINI", "Atlantida", 0, 0, "auction", "random", ""));
        assertThat(filters.q()).isNull();
        assertThat(filters.cat()).isNull();
        assertThat(filters.county()).isNull();
        assertThat(filters.min()).isNull();
        assertThat(filters.max()).isNull();
        assertThat(filters.type()).isNull();
        assertThat(filters.sort()).isNull();
    }

    @Test
    void promptListsEveryCategory() throws Exception {
        String description = SearchInterpreter.Interpretation.class.getDeclaredField("category")
                .getAnnotation(JsonPropertyDescription.class).value();
        Arrays.stream(Category.values()).forEach(category -> assertThat(description).contains(category.name()));
    }

    @Test
    void checksInputAndLimitsRequests() {
        SearchInterpreter interpreter = mock(SearchInterpreter.class);
        when(interpreter.isEnabled()).thenReturn(true);
        when(interpreter.interpret(anyString())).thenReturn(Optional.of(
                new SearchInterpreter.Interpretation("bicicleta", "SPORT", "", 0, 1000, "", "", "Biciclete sub 1000 lei")));
        AiSearchService service = new AiSearchService(interpreter);

        assertThatThrownBy(() -> service.interpret("  a ", "ana")).hasMessageContaining("cel putin 3");
        assertThatThrownBy(() -> service.interpret("x".repeat(201), "ana")).hasMessageContaining("maxim");
        assertThat(service.interpret("o bicicleta ieftina", "ana").cat()).isEqualTo("SPORT");
        for (int i = 1; i < AiSearchService.MAX_REQUESTS_PER_HOUR; i++) {
            service.interpret("o bicicleta ieftina", "ana");
        }
        assertThatThrownBy(() -> service.interpret("o bicicleta ieftina", "ana")).hasMessageContaining("ultima ora");
        assertThat(service.interpret("o bicicleta ieftina", "ion").max()).isEqualByComparingTo("1000");

        when(interpreter.interpret(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.interpret("ceva interzis", "maria")).hasMessageContaining("nu poate cauta");
    }

    @Test
    void disabledWithoutApiKey() {
        AiSearchService service = new AiSearchService(new SearchInterpreter(new ClaudeClient("")));
        assertThatThrownBy(() -> service.interpret("ceas vechi", "ana")).hasMessageContaining("nu este activata");
    }
}
