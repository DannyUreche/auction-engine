package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

// Verifiable auctions: the bids of an item form a SHA-256 chain (written by the database trigger from V39).
// This class recomputes it the same way the browser does (frontend/src/bidChain.js), so a changed, removed or
// reordered bid shows up as a broken link.
@Service
@RequiredArgsConstructor
public class BidChainService {

    public static final String GENESIS = "0".repeat(64);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private final BidRepository bidRepository;

    // valid = every hash matches its bid and links to the previous one; brokenAt = first bad bid id (null if valid)
    public record Verification(Integer itemId, boolean valid, int length, String head, Integer brokenAt, String reason) {
    }

    @Transactional(readOnly = true)
    public Verification verify(Integer itemId) {
        List<Bid> bids = bidRepository.findByItemIdOrderByIdAsc(itemId);
        String previous = GENESIS;
        for (Bid bid : bids) {
            if (!previous.equals(bid.getPrevHash())) {
                return new Verification(itemId, false, bids.size(), null, bid.getId(), "Legatura cu oferta anterioara lipseste");
            }
            if (!hash(bid).equals(bid.getHash())) {
                return new Verification(itemId, false, bids.size(), null, bid.getId(), "Datele ofertei au fost modificate");
            }
            previous = bid.getHash();
        }
        return new Verification(itemId, true, bids.size(), previous, null, null);
    }

    public static String canonical(Integer id, String prevHash, Integer itemId, Integer bidderId, BigDecimal amount,
                                   LocalDateTime createdAt, boolean automatic) {
        return id + "|" + prevHash + "|" + itemId + "|" + bidderId + "|" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + "|" + TIME.format(createdAt) + "|" + automatic;
    }

    static String hash(Bid bid) {
        return sha256(canonical(bid.getId(), bid.getPrevHash(), bid.getItem().getId(), bid.getBidder().getId(), bid.getAmount(),
                bid.getCreatedAt(), bid.isAutomatic()));
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
