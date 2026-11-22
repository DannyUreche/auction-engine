package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Integer> {
}