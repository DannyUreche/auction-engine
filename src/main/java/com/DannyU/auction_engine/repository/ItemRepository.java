package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    // Gaseste toate obiectele care inca sunt active, DAR timpul lor a expirat
    List<Item> findByActiveTrueAndEndTimeBefore(LocalDateTime time);

    // Licitatiile deschise, cele care se termina mai repede primele
    List<Item> findByActiveTrueOrderByEndTimeAsc();

    // Obiectele puse la licitatie de un utilizator, cele mai noi primele
    List<Item> findBySellerUsernameOrderByIdDesc(String username);

    // Obiectele castigate de un utilizator, cele mai recente primele
    List<Item> findByWinnerUsernameOrderByEndTimeDesc(String username);

    // ---------- Pentru pagina de administrare ----------

    List<Item> findAllByOrderByIdDesc();

    long countByActiveTrue();

    long countByWinnerIsNotNull();

    long countBySellerId(Integer sellerId);

    // Vanzari (obiecte cu castigator) pe categorii: [categorie, numar, valoare]
    @Query("SELECT i.category, COUNT(i), SUM(i.currentPrice) FROM Item i WHERE i.winner IS NOT NULL GROUP BY i.category ORDER BY SUM(i.currentPrice) DESC")
    List<Object[]> salesByCategory();

    // Vanzatorii cu cea mai mare valoare vanduta: [username, numar, valoare]
    @Query("SELECT i.seller.username, COUNT(i), SUM(i.currentPrice) FROM Item i WHERE i.winner IS NOT NULL GROUP BY i.seller.username ORDER BY SUM(i.currentPrice) DESC")
    List<Object[]> topSellers();

    // Money reserved by a user: the current price of every running classic auction they are leading
    @Query("SELECT COALESCE(SUM(i.currentPrice), 0) FROM Item i WHERE i.active = true"
            + " AND i.auctionType = com.DannyU.auction_engine.entity.AuctionType.ENGLISH"
            + " AND EXISTS (SELECT b FROM Bid b WHERE b.item = i AND b.bidder.username = :username AND b.amount = i.currentPrice)")
    BigDecimal reservedBy(@Param("username") String username);

    // Scheduled auctions whose start time has come but whose watchers were not told yet
    List<Item> findByActiveTrueAndStartNotifiedFalseAndStartTimeBefore(LocalDateTime time);

    List<Item> findByActiveTrueAndAuctionType(com.DannyU.auction_engine.entity.AuctionType auctionType);

    // Valoarea totala a obiectelor vandute (pretul final al celor cu castigator)
    @Query("SELECT COALESCE(SUM(i.currentPrice), 0) FROM Item i WHERE i.winner IS NOT NULL")
    BigDecimal sumSoldValue();

    // Listings the AI moderation hasn't looked at yet (oldest first)
    List<Item> findTop5ByAiCheckedFalseAndActiveTrueOrderByIdAsc();

    long countByAiCheckedFalseAndActiveTrue();
}
