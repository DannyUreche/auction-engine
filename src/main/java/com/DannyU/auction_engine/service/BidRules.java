package com.DannyU.auction_engine.service;

import java.math.BigDecimal;
import java.time.Duration;

// Regulile licitatiei, intr-un singur loc
public final class BidRules {

    // Ofertele din ultimele 2 minute prelungesc licitatia pana la 2 minute dupa oferta (anti-"sniping")
    public static final Duration ANTI_SNIPING_WINDOW = Duration.ofMinutes(2);

    private BidRules() {
    }

    // Pasul minim de licitare, in trepte dupa pretul curent
    public static BigDecimal increment(BigDecimal currentPrice) {
        if (currentPrice.compareTo(new BigDecimal("100")) < 0) return new BigDecimal("1");
        if (currentPrice.compareTo(new BigDecimal("1000")) < 0) return new BigDecimal("5");
        if (currentPrice.compareTo(new BigDecimal("10000")) < 0) return new BigDecimal("10");
        return new BigDecimal("50");
    }

    // Cea mai mica oferta acceptata acum
    public static BigDecimal minimumNextBid(BigDecimal currentPrice) {
        return currentPrice.add(increment(currentPrice));
    }
}
