package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Integer> {
    // Aduce doar obiectele active a caror data de expirare a trecut
    List<Item> findByActiveTrueAndEndTimeBefore(LocalDateTime now);
}