package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByItemIdOrderByCreatedAtAscIdAsc(Integer itemId);

    // Marks the messages the other participant sent as read; returns how many changed
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
            UPDATE Message m SET m.readAt = :now
            WHERE m.item.id = :itemId AND m.sender.username <> :reader AND m.readAt IS NULL""")
    int markRead(@org.springframework.data.repository.query.Param("itemId") Integer itemId,
                 @org.springframework.data.repository.query.Param("reader") String reader,
                 @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
