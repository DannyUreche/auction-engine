package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    Optional<PurchaseOrder> findByItem_Id(Integer itemId);

    List<PurchaseOrder> findByBuyer_UsernameOrderByCreatedAtDesc(String username);

    List<PurchaseOrder> findBySeller_UsernameOrderByCreatedAtDesc(String username);

    List<PurchaseOrder> findByStatusOrderByDisputedAtAsc(PurchaseOrder.Status status);

    List<PurchaseOrder> findByStatusAndCreatedAtBefore(PurchaseOrder.Status status, LocalDateTime before);

    List<PurchaseOrder> findByStatusAndShippedAtBefore(PurchaseOrder.Status status, LocalDateTime before);

    // Money of a user waiting in escrow: as buyer (paid, not released) or as seller (to be received)
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM PurchaseOrder o WHERE o.buyer.username = :username AND o.status IN ('PAID', 'SHIPPED', 'DISPUTED')")
    BigDecimal heldFor(@Param("username") String username);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM PurchaseOrder o WHERE o.seller.username = :username AND o.status IN ('PAID', 'SHIPPED', 'DISPUTED')")
    BigDecimal pendingFor(@Param("username") String username);

    @Query("SELECT COUNT(o) FROM PurchaseOrder o WHERE (o.buyer.username = :username OR o.seller.username = :username) AND o.status IN ('PAID', 'SHIPPED', 'DISPUTED')")
    long openFor(@Param("username") String username);
}
