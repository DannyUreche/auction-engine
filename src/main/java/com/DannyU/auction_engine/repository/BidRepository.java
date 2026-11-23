package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    // Pentru ruta GET /api/bids/{itemId}
    List<Bid> findByItemIdOrderByAmountDesc(Long itemId);

    // Pentru clasa ta AuctionJob (cauta oferta castigatoare)
    Optional<Bid> findTopByItemIdOrderByAmountDesc(Integer itemId);
}