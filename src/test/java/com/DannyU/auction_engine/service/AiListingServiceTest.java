package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Category;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiListingServiceTest {

    @Test
    void suggestionIsCleanedUp() {
        var listing = AiListingService.toListing(new AiListingService.Suggestion(
                "  Ceas Seiko 5  ", "x".repeat(3000), "electronice", 0));
        assertThat(listing.name()).isEqualTo("Ceas Seiko 5");
        assertThat(listing.description()).hasSize(2000);
        assertThat(listing.category()).isEqualTo(Category.ELECTRONICE);
        assertThat(listing.startPrice()).isEqualByComparingTo(BigDecimal.ONE);

        assertThat(AiListingService.toListing(new AiListingService.Suggestion("A", "B", "NECUNOSCUT", 50)).category())
                .isEqualTo(Category.ALTELE);
    }

    @Test
    void promptListsEveryCategory() throws Exception {
        String description = AiListingService.Suggestion.class.getDeclaredField("category")
                .getAnnotation(JsonPropertyDescription.class).value();
        for (String code : AiListingService.categoryCodes().split(", ")) {
            assertThat(description).contains(code);
        }
    }

    @Test
    void disabledWithoutApiKey() {
        AiListingService service = new AiListingService(null, "");
        assertThat(service.isEnabled()).isFalse();
        assertThatThrownBy(() -> service.suggest(new MockMultipartFile("image", new byte[]{1}), "ana"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nu este activat");
    }
}
