package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Integer> {

    List<Offer> findByItem_IdOrderByCreatedAtDesc(Integer itemId);

    List<Offer> findByItem_IdAndBuyer_UsernameOrderByCreatedAtDesc(Integer itemId, String username);

    // Open offers stop counting once the item gets a bid, sells or closes
    @Modifying
    @Query("UPDATE Offer o SET o.status = com.DannyU.auction_engine.entity.Offer.Status.EXPIRED, o.respondedAt = CURRENT_TIMESTAMP"
            + " WHERE o.item.id = :itemId AND o.status IN (com.DannyU.auction_engine.entity.Offer.Status.PENDING,"
            + " com.DannyU.auction_engine.entity.Offer.Status.COUNTERED)")
    int expireOpen(@Param("itemId") Integer itemId);
}
