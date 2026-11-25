package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.ImageThumbnail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageThumbnailRepository extends JpaRepository<ImageThumbnail, String> {
}
