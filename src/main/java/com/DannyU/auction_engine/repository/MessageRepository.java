package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByItemIdOrderByCreatedAtAscIdAsc(Integer itemId);
}
