package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    boolean existsByItemIdAndAuthorUsername(Integer itemId, String username);

    List<Review> findByItemIdOrderByCreatedAtAsc(Integer itemId);

    List<Review> findTop20ByTargetUsernameOrderByCreatedAtDesc(String username);

    long countByTargetUsername(String username);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.target.username = :username")
    double averageFor(@Param("username") String username);
}
