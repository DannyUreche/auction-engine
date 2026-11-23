package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuctionJob {

    private final ItemRepository itemRepository;

    // Aceasta metoda va rula complet automat o data la 60.000 milisecunde (1 minut)
    @Scheduled(fixedRate = 60000)
    public void verifyExpiredAuctions() {
        System.out.println("🤖 [ROBOT] Verific licitatiile expirate...");

        List<Item> allItems = itemRepository.findAll();

        for (Item item : allItems) {
            // Daca licitatia este inca activa SI timpul ei a expirat
            if (item.getActive() && LocalDateTime.now().isAfter(item.getEndTime())) {

                // O trecem pe inactiva (inchisa)
                item.setActive(false);
                itemRepository.save(item);

                // Anuntam castigatorul in consola (si teoretic am putea trimite un email aici)
                System.out.println("🎉 [LICITATIE INCHEIATA] Obiectul '" + item.getName() +
                        "' a fost adjudecat pentru suma de: " + item.getCurrentPrice());
            }
        }
    }
}