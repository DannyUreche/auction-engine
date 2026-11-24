package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuctionScheduler {

    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    // Se executa automat o data la 60.000 milisecunde (1 minut)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredAuctions() {
        // 1. Cautam toate licitatiile care inca apar ca active, dar au trecut de endTime
        List<Item> expiredItems = itemRepository.findByActiveTrueAndEndTimeBefore(LocalDateTime.now());

        for (Item item : expiredItems) {
            closeAuction(item);
        }
    }

    // Inchide o licitatie si stabileste castigatorul. Folosita si de administrator ("Inchide acum").
    @Transactional
    public void closeAuction(Item item) {
        // 2. Cautam oferta cea mai mare
        Optional<Bid> winningBid = bidRepository.findTopByItemIdOrderByAmountDesc(item.getId());

        // 3. Inchidem licitatia si salvam castigatorul (daca exista)
        item.setActive(false);
        winningBid.ifPresent(bid -> item.setWinner(bid.getBidder()));
        itemRepository.save(item);

        // 4. Anuntam browserele conectate (se trimite abia dupa salvarea tranzactiei)
        eventPublisher.publishEvent(AuctionEvent.closed(item.getId(), item.getCurrentPrice(),
                item.getWinner() == null ? null : item.getWinner().getUsername()));

        // 5. Notificari pentru castigator si vanzator
        if (winningBid.isPresent()) {
            notificationService.notify(item.getWinner(), Notification.Type.WON, item,
                    "Ai castigat \"" + item.getName() + "\" cu " + item.getCurrentPrice() + " RON! Vezi datele de contact ale vanzatorului.");
            notificationService.notify(item.getSeller(), Notification.Type.SOLD, item,
                    "\"" + item.getName() + "\" s-a vandut cu " + item.getCurrentPrice() + " RON lui " + item.getWinner().getUsername() + ".");
        } else {
            notificationService.notify(item.getSeller(), Notification.Type.UNSOLD, item,
                    "Licitatia pentru \"" + item.getName() + "\" s-a incheiat fara oferte.");
        }

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
