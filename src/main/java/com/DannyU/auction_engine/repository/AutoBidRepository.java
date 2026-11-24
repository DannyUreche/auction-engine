package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.AutoBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AutoBidRepository extends JpaRepository<AutoBid, Integer> {

    Optional<AutoBid> findByItemIdAndUserUsername(Integer itemId, String username);

    // Cel mai mare maxim primul; la egalitate, cel setat mai devreme
    List<AutoBid> findByItemIdOrderByMaxAmountDescCreatedAtAsc(Integer itemId);
}
