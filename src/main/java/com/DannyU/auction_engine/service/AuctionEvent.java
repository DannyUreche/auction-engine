package com.DannyU.auction_engine.service;

import java.math.BigDecimal;

/**
 * Ceva s-a schimbat la o licitatie si browserele conectate trebuie anuntate.
 *
 * @param type     "bid" (oferta noua) sau "closed" (licitatie inchisa)
 * @param itemId   obiectul afectat
 * @param amount   suma ofertei / pretul final
 * @param username cine a licitat / cine a castigat (null daca licitatia s-a inchis fara oferte)
 */
public record AuctionEvent(String type, Integer itemId, BigDecimal amount, String username) {

    public static AuctionEvent bid(Integer itemId, BigDecimal amount, String username) {
        return new AuctionEvent("bid", itemId, amount, username);
    }

    public static AuctionEvent closed(Integer itemId, BigDecimal finalPrice, String winnerUsername) {
        return new AuctionEvent("closed", itemId, finalPrice, winnerUsername);
    }
}
