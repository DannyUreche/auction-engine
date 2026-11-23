package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemImageRepository extends JpaRepository<ItemImage, String> {
}
