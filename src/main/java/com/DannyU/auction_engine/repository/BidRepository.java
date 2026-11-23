package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Integer> {
    // Gaseste oferta cu cea mai mare suma pentru un anumit obiect
    Optional<Bid> findTopByItemIdOrderByAmountDesc(Integer itemId);
}