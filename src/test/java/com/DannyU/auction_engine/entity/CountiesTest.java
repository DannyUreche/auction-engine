package com.DannyU.auction_engine.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountiesTest {

    @Test
    void fortyOneCountiesAndBucharest() {
        assertThat(Counties.ALL).hasSize(42).doesNotHaveDuplicates().contains("Bucuresti", "Suceava", "Satu Mare");
    }

    @Test
    void anySpellingBecomesTheOfficialName() {
        assertThat(Counties.canonical("  timiș ")).isEqualTo("Timis");
        assertThat(Counties.canonical("BISTRITA NASAUD")).isEqualTo("Bistrita-Nasaud");
        assertThat(Counties.canonical("satu-mare")).isEqualTo("Satu Mare");
        assertThat(Counties.canonical("")).isNull();
        assertThatThrownBy(() -> Counties.canonical("Cluj-Napoca")).isInstanceOf(IllegalArgumentException.class);
    }
}
