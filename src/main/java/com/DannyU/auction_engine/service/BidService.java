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

@Service // Ii spune lui Spring ca aceasta clasa contine logica de business
@RequiredArgsConstructor // Lombok genereaza automat constructorul pentru noi
public class BidService {

    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Transactional // Daca pica vreo eroare, nicio modificare nu se salveaza in baza de date (Rollback)
    public Bid placeBid(Integer itemId, Integer bidderId, BigDecimal bidAmount) {

        // 1. Cautam obiectul scos la licitatie
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));

        // 2. Cautam utilizatorul care vrea sa liciteze
        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));

        // 3. Optional, dar logic: Vanzatorul nu poate licita pentru propriul obiect
        if (item.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("Nu poti licita pentru propriul obiect!");
        }

        // 4. VERIFICAREA PRINCIPALA: Suma trebuie sa fie mai mare decat pretul curent
        // Folosim compareTo() pentru ca BigDecimal nu suporta semnele > sau < direct
        if (bidAmount.compareTo(item.getCurrentPrice()) <= 0) {
            throw new IllegalArgumentException("Oferta respinsa! Suma trebuie sa fie mai mare de " + item.getCurrentPrice());
        }

        // 5. Actualizam noul pret curent al obiectului
        item.setCurrentPrice(bidAmount);
        itemRepository.save(item);

        // 6. Salvam licitatia in istoric (tabela bids)
        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(bidder);
        bid.setAmount(bidAmount);

        return bidRepository.save(bid);
    }
}