package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final AuctionScheduler auctionScheduler;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Bid placeBid(Integer itemId, String bidderUsername, BigDecimal bidAmount) {
        Item item = findItem(itemId);
        User bidder = checkCanBuy(item, bidderUsername);

        // Oferta trebuie sa fie cel putin pretul curent + pasul minim (vezi BidRules)
        BigDecimal minimum = BidRules.minimumNextBid(item.getCurrentPrice());
        if (bidAmount.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("Oferta respinsa! Oferta minima este " + minimum
                    + " RON (pretul curent + pasul de " + BidRules.increment(item.getCurrentPrice()) + " RON)");
        }

        // Anti-"sniping": o oferta in ultimele minute prelungeste licitatia
        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(now, item.getEndTime()).compareTo(BidRules.ANTI_SNIPING_WINDOW) < 0) {
            item.setEndTime(now.plus(BidRules.ANTI_SNIPING_WINDOW));
        }

        return saveBid(item, bidder, bidAmount);
    }

    // "Cumpara acum": oferta la pretul fix si licitatia se inchide imediat, cu cumparatorul castigator
    @Transactional
    public Bid buyNow(Integer itemId, String buyerUsername) {
        Item item = findItem(itemId);
        User buyer = checkCanBuy(item, buyerUsername);

        if (item.getBuyNowPrice() == null) {
            throw new IllegalArgumentException("Acest obiect nu are pret de cumparare imediata!");
        }
        if (!item.isBuyNowAvailable()) {
            throw new IllegalArgumentException("Cumpararea imediata nu mai este disponibila: ofertele au ajuns la acest pret!");
        }

        Bid bid = saveBid(item, buyer, item.getBuyNowPrice());
        auctionScheduler.closeAuction(item);
        return bid;
    }

    public List<Bid> getBidsForItem(Integer itemId) {
        return bidRepository.findByItemIdOrderByAmountDesc(itemId);
    }

    public List<Bid> getBidsForUser(String bidderUsername) {
        return bidRepository.findByBidderUsernameOrderByCreatedAtDesc(bidderUsername);
    }

    private Item findItem(Integer itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
    }

    // Regulile comune pentru licitare si "Cumpara acum"
    private User checkCanBuy(Item item, String username) {
        // Obiectul mai este activ?
        if (!item.getActive()) {
            throw new IllegalArgumentException("Aceasta licitatie a fost inchisa oficial!");
        }
        // A expirat timpul licitatiei?
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new IllegalArgumentException("Licitatia pentru acest obiect s-a incheiat deja!");
        }
        // Utilizatorul autentificat, pe baza username-ului din token-ul JWT
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        // Vanzatorul nu liciteaza la propriul obiect
        if (item.getSeller().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Nu poti licita pentru propriul obiect!");
        }
        return user;
    }

    private Bid saveBid(Item item, User bidder, BigDecimal amount) {
        item.setCurrentPrice(amount);
        itemRepository.save(item);

        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        Bid saved = bidRepository.save(bid);

        // Anuntam browserele conectate (se trimite abia dupa salvarea tranzactiei)
        eventPublisher.publishEvent(AuctionEvent.bid(item.getId(), amount, bidder.getUsername()));
        return saved;
    }
}
