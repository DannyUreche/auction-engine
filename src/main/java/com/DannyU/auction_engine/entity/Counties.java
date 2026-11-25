package com.DannyU.auction_engine.entity;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

// Romania's 41 counties + Bucuresti, written without diacritics (the frontend has the same list in counties.js)
public final class Counties {

    public static final List<String> ALL = List.of(
            "Alba", "Arad", "Arges", "Bacau", "Bihor", "Bistrita-Nasaud", "Botosani", "Braila", "Brasov", "Bucuresti",
            "Buzau", "Calarasi", "Caras-Severin", "Cluj", "Constanta", "Covasna", "Dambovita", "Dolj", "Galati", "Giurgiu",
            "Gorj", "Harghita", "Hunedoara", "Ialomita", "Iasi", "Ilfov", "Maramures", "Mehedinti", "Mures", "Neamt", "Olt",
            "Prahova", "Salaj", "Satu Mare", "Sibiu", "Suceava", "Teleorman", "Timis", "Tulcea", "Valcea", "Vaslui", "Vrancea");

    private Counties() {
    }

    // The official spelling of a county typed in any case, with or without diacritics; null if blank.
    // Throws for names that aren't a county.
    public static String canonical(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = simplify(name);
        return ALL.stream().filter(county -> simplify(county).equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Judetul \"" + name.trim() + "\" nu exista!"));
    }

    private static String simplify(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "");
    }
}
