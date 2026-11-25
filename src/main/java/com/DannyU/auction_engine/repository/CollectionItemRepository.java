package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.CollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollectionItemRepository extends JpaRepository<CollectionItem, Integer> {

    List<CollectionItem> findByCollection_IdOrderByAddedAtDesc(Integer collectionId);

    Optional<CollectionItem> findByCollection_IdAndItem_Id(Integer collectionId, Integer itemId);

    long countByCollection_Id(Integer collectionId);

    @Query("SELECT ci.collection.id FROM CollectionItem ci WHERE ci.item.id = :itemId AND ci.collection.owner.id = :ownerId")
    List<Integer> collectionIdsWithItem(@Param("ownerId") Integer ownerId, @Param("itemId") Integer itemId);
}
