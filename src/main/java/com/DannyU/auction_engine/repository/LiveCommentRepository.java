package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.LiveComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveCommentRepository extends JpaRepository<LiveComment, Integer> {

    // The newest 50 (the frontend shows them oldest first)
    List<LiveComment> findTop50ByItemIdOrderByCreatedAtDesc(Integer itemId);
}
