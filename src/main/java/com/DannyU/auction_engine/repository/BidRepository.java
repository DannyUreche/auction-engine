package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Ultimele oferte de pe tot site-ul (fluxul "Activitate live" de pe pagina principala)
    List<Bid> findTop8ByOrderByCreatedAtDesc();

    // Pentru pagina de administrare

    // Numarul de oferte pe zi, incepand cu o data (graficul "Oferte pe zi")
    @Query(value = "SELECT CAST(created_at AS DATE) AS day, COUNT(*) AS bids FROM bids WHERE created_at >= :from GROUP BY day ORDER BY day",
            nativeQuery = true)
    List<Object[]> countPerDaySince(@Param("from") java.time.LocalDateTime from);

    long countByBidderId(Integer bidderId);

    long countByItemId(Integer itemId);

    // Sterge toate ofertele unui obiect (inainte de stergerea obiectului de catre admin).
    // clearAutomatically: ofertele sterse dispar si din memoria Hibernate, ca sa nu mai fie salvate la loc
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Bid b WHERE b.item.id = :itemId")
    int deleteAllForItem(@Param("itemId") Integer itemId);
}