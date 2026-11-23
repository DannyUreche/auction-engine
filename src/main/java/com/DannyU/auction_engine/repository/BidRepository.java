package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Integer> {

    // Pentru ruta GET /api/bids/{itemId}
    List<Bid> findByItemIdOrderByAmountDesc(Integer itemId);

    // Pentru AuctionScheduler (cauta oferta castigatoare)
    Optional<Bid> findTopByItemIdOrderByAmountDesc(Integer itemId);

    // Pentru ruta GET /api/bids/my (ofertele utilizatorului logat, cele mai noi primele)
    List<Bid> findByBidderUsernameOrderByCreatedAtDesc(String username);
}