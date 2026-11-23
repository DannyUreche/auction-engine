package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.WatchedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchedItemRepository extends JpaRepository<WatchedItem, Integer> {

    Optional<WatchedItem> findByUserUsernameAndItemId(String username, Integer itemId);

    // Obiectele urmarite de un utilizator, cele adaugate recent primele
    @Query("SELECT w.item FROM WatchedItem w WHERE w.user.username = :username ORDER BY w.createdAt DESC, w.id DESC")
    List<Item> findItemsWatchedBy(@Param("username") String username);
}
