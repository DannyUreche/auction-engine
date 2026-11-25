package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AuctionType;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.Offer;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.OfferRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// "Fa o oferta": private price negotiation on a classic auction that has no bids yet. The seller accepts,
// rejects or counters; the buyer can accept a counter offer or withdraw. Accepting sells the item right away at
// the agreed price (same path as "Cumpara acum": a bid at that price, then the auction closes).
@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final BidService bidService;
    private final WalletService walletService;
    private final NotificationService notificationService;

    public enum Action { ACCEPT, REJECT, COUNTER, WITHDRAW }

    // The seller sees every offer on the item, a buyer only their own
    @Transactional(readOnly = true)
    public List<Offer> offersFor(Integer itemId, String username) {
        Item item = findItem(itemId);
        return item.getSeller().getUsername().equals(username)
                ? offerRepository.findByItem_IdOrderByCreatedAtDesc(itemId)
                : offerRepository.findByItem_IdAndBuyer_UsernameOrderByCreatedAtDesc(itemId, username);
    }

    @Transactional
    public Offer make(Integer itemId, String username, BigDecimal amount, String message) {
        Item item = findItem(itemId);
        User buyer = findUser(username);
        checkNegotiable(item);
        if (item.getSeller().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("Nu poti face o oferta la propriul obiect!");
        }
        if (item.getBuyNowPrice() != null && amount.compareTo(item.getBuyNowPrice()) >= 0) {
            throw new IllegalArgumentException("Pentru acest pret foloseste direct \"Cumpara acum\"!");
        }
        boolean hasOpen = offerRepository.findByItem_IdAndBuyer_UsernameOrderByCreatedAtDesc(itemId, username).stream().anyMatch(Offer::isOpen);
        if (hasOpen) {
            throw new IllegalArgumentException("Ai deja o oferta in asteptare pentru acest obiect.");
        }
        walletService.checkFunds(buyer, item, amount);

        Offer offer = new Offer();
        offer.setItem(item);
        offer.setBuyer(buyer);
        offer.setAmount(amount);
        offer.setMessage(message == null || message.isBlank() ? null : message.trim());
        offerRepository.save(offer);
        notificationService.notify(item.getSeller(), Notification.Type.OFFER, item,
                buyer.getUsername() + " iti ofera " + plain(amount) + " RON pentru \"" + item.getName() + "\"");
        return offer;
    }

    @Transactional
    public Offer respond(Integer offerId, String username, Action action, BigDecimal counterAmount) {
        Offer offer = offerRepository.findById(offerId).orElseThrow(() -> new IllegalArgumentException("Oferta nu a fost gasita!"));
        Item item = offer.getItem();
        boolean seller = item.getSeller().getUsername().equals(username);
        boolean buyer = offer.getBuyer().getUsername().equals(username);
        if (!seller && !buyer) {
            throw new IllegalArgumentException("Nu poti raspunde la aceasta oferta!");
        }
        if (!offer.isOpen()) {
            throw new IllegalArgumentException("Oferta nu mai este activa.");
        }

        switch (action) {
            case ACCEPT -> {
                // The seller accepts the buyer's price; the buyer accepts the seller's counter offer
                if (seller && offer.getStatus() != Offer.Status.PENDING || buyer && offer.getStatus() != Offer.Status.COUNTERED) {
                    throw new IllegalArgumentException("Acum este randul celeilalte parti sa raspunda.");
                }
                checkNegotiable(item);
                BigDecimal price = seller ? offer.getAmount() : offer.getCounterAmount();
                close(offer, Offer.Status.ACCEPTED);
                offerRepository.expireOpen(item.getId());
                bidService.sellAtAgreedPrice(item, offer.getBuyer(), price);
            }
            case REJECT -> {
                close(offer, Offer.Status.REJECTED);
                User other = seller ? offer.getBuyer() : item.getSeller();
                notificationService.notify(other, Notification.Type.OFFER_REPLY, item,
                        (seller ? "Vanzatorul a refuzat oferta ta" : offer.getBuyer().getUsername() + " a refuzat contraoferta ta")
                                + " pentru \"" + item.getName() + "\"");
            }
            case COUNTER -> {
                if (!seller || offer.getStatus() != Offer.Status.PENDING) {
                    throw new IllegalArgumentException("Doar vanzatorul poate face o contraoferta, o singura data.");
                }
                if (counterAmount == null || counterAmount.compareTo(offer.getAmount()) <= 0) {
                    throw new IllegalArgumentException("Contraoferta trebuie sa fie mai mare decat oferta primita!");
                }
                if (item.getBuyNowPrice() != null && counterAmount.compareTo(item.getBuyNowPrice()) > 0) {
                    throw new IllegalArgumentException("Contraoferta nu poate depasi pretul \"Cumpara acum\"!");
                }
                offer.setCounterAmount(counterAmount);
                offer.setStatus(Offer.Status.COUNTERED);
                offer.setRespondedAt(LocalDateTime.now());
                notificationService.notify(offer.getBuyer(), Notification.Type.OFFER_REPLY, item,
                        "Contraoferta pentru \"" + item.getName() + "\": " + plain(counterAmount) + " RON");
            }
            case WITHDRAW -> {
                if (!buyer) {
                    throw new IllegalArgumentException("Doar cumparatorul isi poate retrage oferta.");
                }
                close(offer, Offer.Status.WITHDRAWN);
            }
        }
        return offer;
    }

    // Only classic auctions that are still open and have no bids can be negotiated
    private void checkNegotiable(Item item) {
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new IllegalArgumentException("Licitatia s-a incheiat!");
        }
        if (item.getAuctionType() == AuctionType.DUTCH) {
            throw new IllegalArgumentException("La licitatia olandeza nu se negociaza: cumperi la pretul curent!");
        }
        if (bidRepository.countByItemId(item.getId()) > 0) {
            throw new IllegalArgumentException("Obiectul are deja oferte in licitatie, nu se mai poate negocia.");
        }
    }

    private void close(Offer offer, Offer.Status status) {
        offer.setStatus(status);
        offer.setRespondedAt(LocalDateTime.now());
        offerRepository.save(offer);
    }

    private Item findItem(Integer id) {
        return itemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
    }

    private static String plain(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
