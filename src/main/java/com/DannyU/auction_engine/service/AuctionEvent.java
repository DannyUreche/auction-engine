package com.DannyU.auction_engine.service;

import java.math.BigDecimal;

/**
 * Ceva s-a schimbat la o licitatie si browserele conectate trebuie anuntate.
 *
 * @param type     "bid" (oferta noua), "closed" (licitatie inchisa), "deleted" (obiect sters de admin)
 *                 "notification" (utilizatorul username are o notificare noua; itemId e null)
 *                 sau "message" (mesaj nou in chat-ul obiectului itemId pentru utilizatorul username)
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

    // Obiectul a fost sters de un administrator
    public static AuctionEvent deleted(Integer itemId) {
        return new AuctionEvent("deleted", itemId, null, null);
    }

    // Utilizatorul are o notificare noua (browserul lui isi reincarca lista)
    public static AuctionEvent notification(String username) {
        return new AuctionEvent("notification", null, null, username);
    }

    // An item changed without a bid (a scheduled auction started, a Dutch price dropped): browsers reload it
    public static AuctionEvent update(Integer itemId, BigDecimal price) {
        return new AuctionEvent("update", itemId, price, null);
    }

    // New message in the public live chat of an item (no text: browsers reload the comments)
    public static AuctionEvent comment(Integer itemId, String author) {
        return new AuctionEvent("comment", itemId, null, author);
    }

    // Mesaj nou in chat pentru destinatar (fara text: browserul lui isi cere mesajele)
    public static AuctionEvent message(Integer itemId, String recipientUsername) {
        return new AuctionEvent("message", itemId, null, recipientUsername);
    }

    // The other chat participant is typing (sent to the recipient; nothing stored)
    public static AuctionEvent typing(Integer itemId, String recipientUsername) {
        return new AuctionEvent("typing", itemId, null, recipientUsername);
    }

    // The recipient read the messages of username (who then shows the read receipts)
    public static AuctionEvent read(Integer itemId, String senderUsername) {
        return new AuctionEvent("read", itemId, null, senderUsername);
    }

    // Live show video: peer has new signaling messages waiting (itemId = show id; no content, see LiveStreamService)
    public static AuctionEvent rtc(Integer showId, String peer) {
        return new AuctionEvent("rtc", showId, null, peer);
    }

    // Live show video started (amount 1) or stopped (amount 0); itemId = show id
    public static AuctionEvent stream(Integer showId, boolean live) {
        return new AuctionEvent("stream", showId, live ? BigDecimal.ONE : BigDecimal.ZERO, null);
    }
}
