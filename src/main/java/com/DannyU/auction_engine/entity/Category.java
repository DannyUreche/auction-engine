package com.DannyU.auction_engine.entity;

// Categoriile obiectelor. In baza de date se salveaza numele constantei (ex: "ELECTRONICE").
public enum Category {
    ELECTRONICE("Electronice", "📱"),
    CEASURI_BIJUTERII("Ceasuri si bijuterii", "⌚"),
    ARTA_COLECTII("Arta si colectii", "🎨"),
    MODA("Moda", "👕"),
    CASA_GRADINA("Casa si gradina", "🏡"),
    SPORT("Sport", "⚽"),
    CARTI_MUZICA("Carti si muzica", "📚"),
    VEHICULE("Vehicule", "🚲"),
    ALTELE("Altele", "📦");

    private final String label;
    private final String icon;

    Category(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }
}
