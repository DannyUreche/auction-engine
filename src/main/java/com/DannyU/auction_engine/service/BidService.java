package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.entity.AuctionType;
import com.DannyU.auction_engine.entity.AutoBid;
import com.DannyU.auction_engine.repository.AutoBidRepository;
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
    private final AutoBidRepository autoBidRepository;
    private final NotificationService notificationService;
    private final WalletService walletService;

    @Transactional
    public Bid placeBid(Integer itemId, String bidderUsername, BigDecimal bidAmount) {
        Item item = findItem(itemId);
        User bidder = checkCanBuy(item, bidderUsername);
        rejectDutch(item);

        // Oferta trebuie sa fie cel putin pretul curent + pasul minim (vezi BidRules)
        BigDecimal minimum = BidRules.minimumNextBid(item.getCurrentPrice());
        if (bidAmount.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("Oferta respinsa! Oferta minima este " + minimum
                    + " RON (pretul curent + pasul de " + BidRules.increment(item.getCurrentPrice()) + " RON)");
        }
        walletService.checkFunds(bidder, item, bidAmount);

        Bid bid = saveBid(item, bidder, bidAmount, false);
        // Cei cu licitare automata raspund imediat (oferta manuala poate fi depasita pe loc)
        resolveAutoBids(item);
        return bid;
    }

    // ---------- Licitare automata ----------

    // Seteaza (sau schimba) suma maxima pana la care aplicatia liciteaza pentru utilizator
    @Transactional
    public AutoBid setAutoBid(Integer itemId, String username, BigDecimal maxAmount) {
        Item item = findItem(itemId);
        User user = checkCanBuy(item, username);
        rejectDutch(item);
        BigDecimal minimum = BidRules.minimumNextBid(item.getCurrentPrice());
        if (maxAmount.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("Suma maxima trebuie sa fie cel putin " + minimum + " RON!");
        }

        AutoBid autoBid = autoBidRepository.findByItemIdAndUserUsername(itemId, username).orElseGet(() -> {
            AutoBid created = new AutoBid();
            created.setItem(item);
            created.setUser(user);
            return created;
        });
        autoBid.setMaxAmount(maxAmount);
        AutoBid saved = autoBidRepository.save(autoBid);

        // Daca altcineva conduce, licitarea automata porneste imediat
        resolveAutoBids(item);
        return saved;
    }

    @Transactional
    public void cancelAutoBid(Integer itemId, String username) {
        autoBidRepository.findByItemIdAndUserUsername(itemId, username).ifPresent(autoBidRepository::delete);
    }

    public java.util.Optional<AutoBid> getAutoBid(Integer itemId, String username) {
        return autoBidRepository.findByItemIdAndUserUsername(itemId, username);
    }

    // Cat timp cineva cu licitare automata nu conduce si maximul ii permite, liciteaza pentru el
    // exact cat trebuie ca sa treaca de cel care conduce (fara sa depaseasca propriul maxim).
    // Exemplu: A are maxim 500, B are maxim 300, pretul e 100 -> B liciteaza 300, A raspunde cu 305 si castiga.
    private void resolveAutoBids(Item item) {
        // Limita de siguranta; in practica se opreste dupa 1-3 oferte
        for (int round = 0; round < 50; round++) {
            if (!item.getActive() || LocalDateTime.now().isAfter(item.getEndTime())) {
                return;
            }
            String leader = bidRepository.findTopByItemIdOrderByAmountDesc(item.getId())
                    .map(b -> b.getBidder().getUsername()).orElse(null);
            BigDecimal minimum = BidRules.minimumNextBid(item.getCurrentPrice());
            List<AutoBid> autoBids = autoBidRepository.findByItemIdOrderByMaxAmountDescCreatedAtAsc(item.getId());

            // An auto-bid can only go as high as its owner's wallet allows
            AutoBid challenger = autoBids.stream()
                    .filter(a -> !a.getUser().getUsername().equals(leader))
                    .filter(a -> effectiveMax(a, item).compareTo(minimum) >= 0)
                    .findFirst().orElse(null);
            if (challenger == null) {
                return;
            }

            // Cat trebuie oferit ca sa treaca si de maximul (secret) al celui care conduce
            BigDecimal target = autoBids.stream()
                    .filter(a -> a.getUser().getUsername().equals(leader))
                    .map(a -> effectiveMax(a, item))
                    .map(max -> max.compareTo(minimum) >= 0 ? BidRules.minimumNextBid(max) : minimum)
                    .findFirst().orElse(minimum);
            saveBid(item, challenger.getUser(), effectiveMax(challenger, item).min(target), true);
        }
    }

    // "Cumpara acum": oferta la pretul fix si licitatia se inchide imediat, cu cumparatorul castigator
    @Transactional
    public Bid buyNow(Integer itemId, String buyerUsername) {
        Item item = findItem(itemId);
        User buyer = checkCanBuy(item, buyerUsername);

        // Dutch auction: you buy at the current (dropping) price
        BigDecimal price;
        if (item.getAuctionType() == AuctionType.DUTCH) {
            price = item.dutchPriceAt(LocalDateTime.now());
        } else {
            if (item.getBuyNowPrice() == null) {
                throw new IllegalArgumentException("Acest obiect nu are pret de cumparare imediata!");
            }
            if (!item.isBuyNowAvailable()) {
                throw new IllegalArgumentException("Cumpararea imediata nu mai este disponibila: ofertele au ajuns la acest pret!");
            }
            price = item.getBuyNowPrice();
        }
        walletService.checkFunds(buyer, item, price);

        Bid bid = saveBid(item, buyer, price, false);
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
        // Scheduled auction that has not opened yet
        if (!item.isStarted()) {
            throw new IllegalArgumentException("Licitatia nu a inceput inca!");
        }
        // Vanzatorul nu liciteaza la propriul obiect
        if (item.getSeller().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Nu poti licita pentru propriul obiect!");
        }
        return user;
    }

    private void rejectDutch(Item item) {
        if (item.getAuctionType() == AuctionType.DUTCH) {
            throw new IllegalArgumentException("La licitatia olandeza nu se liciteaza: cumperi la pretul curent!");
        }
    }

    // The auto-bid maximum, limited to what the owner can still afford
    private BigDecimal effectiveMax(AutoBid autoBid, Item item) {
        return autoBid.getMaxAmount().min(walletService.availableFor(autoBid.getUser(), item));
    }

    private Bid saveBid(Item item, User bidder, BigDecimal amount, boolean automatic) {
        // Anti-"sniping": o oferta in ultimele minute prelungeste licitatia
        LocalDateTime now = LocalDateTime.now();
        if (item.getActive()
                && Duration.between(now, item.getEndTime()).compareTo(BidRules.ANTI_SNIPING_WINDOW) < 0) {
            item.setEndTime(now.plus(BidRules.ANTI_SNIPING_WINDOW));
        }
        // Cine conducea inainte de aceasta oferta (primeste notificarea "oferta depasita")
        User previousLeader = bidRepository.findTopByItemIdOrderByAmountDesc(item.getId())
                .map(Bid::getBidder).orElse(null);

        item.setCurrentPrice(amount);
        itemRepository.save(item);

        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setAutomatic(automatic);
        Bid saved = bidRepository.save(bid);

        // Anuntam browserele conectate (se trimite abia dupa salvarea tranzactiei)
        eventPublisher.publishEvent(AuctionEvent.bid(item.getId(), amount, bidder.getUsername()));

        if (previousLeader != null && !previousLeader.getId().equals(bidder.getId())) {
            notificationService.notify(previousLeader, Notification.Type.OUTBID, item,
                    "Oferta ta la \"" + item.getName() + "\" a fost depasita: " + amount + " RON de la " + bidder.getUsername());
        }
        notificationService.notify(item.getSeller(), Notification.Type.NEW_BID, item,
                "Oferta noua la \"" + item.getName() + "\": " + amount + " RON de la " + bidder.getUsername());
        return saved;
    }
}
