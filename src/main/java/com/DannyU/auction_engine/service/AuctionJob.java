package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuctionJob {

    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;

    @Scheduled(fixedRate = 60000)
    public void verifyExpiredAuctions() {
        // Cerem bazei de date STRICT obiectele care trebuie inchise acum
        List<Item> expiredItems = itemRepository.findByActiveTrueAndEndTimeBefore(LocalDateTime.now());

        if (expiredItems.isEmpty()) {
            return; // Daca nu a expirat nimic, robotul nu mai incarca consola
        }

        for (Item item : expiredItems) {
            item.setActive(false);
            itemRepository.save(item);

            // Cautam cine a pus cea mai mare oferta
            Optional<Bid> winningBid = bidRepository.findTopByItemIdOrderByAmountDesc(item.getId());

            if (winningBid.isPresent()) {
                Bid winner = winningBid.get();
                System.out.println("🎉 [LICITATIE INCHEIATA] '" + item.getName() +
                        "' a fost castigat de " + winner.getBidder().getUsername() +
                        " cu suma de " + winner.getAmount() + "!");
            } else {
                System.out.println("⏳ [LICITATIE INCHEIATA] '" + item.getName() +
                        "' a expirat fara nicio oferta.");
            }
        }
    }
}