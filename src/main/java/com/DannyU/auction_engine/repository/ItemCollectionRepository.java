package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.ItemCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCollectionRepository extends JpaRepository<ItemCollection, Integer> {

    List<ItemCollection> findByOwner_IdOrderByUpdatedAtDesc(Integer ownerId);

    List<ItemCollection> findByOwner_IdAndPublicListTrueOrderByUpdatedAtDesc(Integer ownerId);

    long countByOwner_Id(Integer ownerId);
}
