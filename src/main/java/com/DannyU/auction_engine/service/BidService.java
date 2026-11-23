package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Transactional
    public Bid placeBid(Integer itemId, String bidderUsername, BigDecimal bidAmount) {

        // 1. Cautam obiectul
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));

        // 2. A expirat timpul licitatiei?
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new IllegalArgumentException("Licitatia pentru acest obiect s-a incheiat deja!");
        }

        // 3. Cautam utilizatorul autentificat pe baza username-ului din token-ul JWT
        User bidder = userRepository.findByUsername(bidderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));

        // 4. Vanzatorul nu liciteaza la propriul obiect
        if (item.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("Nu poti licita pentru propriul obiect!");
        }

        // 5. Oferta e mai mare?
        if (bidAmount.compareTo(item.getCurrentPrice()) <= 0) {
            throw new IllegalArgumentException("Oferta respinsa! Suma trebuie sa fie mai mare de " + item.getCurrentPrice());
        }

        // 6. Salvam datele
        item.setCurrentPrice(bidAmount);
        itemRepository.save(item);

        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(bidder);
        bid.setAmount(bidAmount);

        return bidRepository.save(bid);
    }
}