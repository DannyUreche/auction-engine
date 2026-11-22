package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Integer> {
}