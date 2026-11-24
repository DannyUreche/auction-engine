package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findTop30ByUserUsernameOrderByCreatedAtDescIdDesc(String username);

    long countByUserUsernameAndReadFalse(String username);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = (SELECT u.id FROM User u WHERE u.username = :username)")
    int markAllRead(@Param("username") String username);
}
