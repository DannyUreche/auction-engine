package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionScheduler {

    private final ItemRepository itemRepository;

    // Se executa automat o data la 60.000 milisecunde (1 minut)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredAuctions() {
        System.out.println("⏳ [Scheduler] Verific licitatiile expirate... " + LocalDateTime.now());

        // 1. Cautam toate licitatiile care inca apar ca active, dar au trecut de endTime
        List<Item> expiredItems = itemRepository.findByActiveTrueAndEndTimeBefore(LocalDateTime.now());

        for (Item item : expiredItems) {
            // 2. Le marcam ca inactive
            item.setActive(false);
            itemRepository.save(item);

            // 3. Printam un mesaj de confirmare
            System.out.println("🔒 Licitatia pentru obiectul [" + item.getName() + "] s-a incheiat!");
            System.out.println("💰 Pret final: " + item.getCurrentPrice());
        }
    }
}