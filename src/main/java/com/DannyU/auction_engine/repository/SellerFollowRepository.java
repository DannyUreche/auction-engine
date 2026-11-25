package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.SellerFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerFollowRepository extends JpaRepository<SellerFollow, Integer> {

    Optional<SellerFollow> findByFollower_UsernameAndSeller_Username(String follower, String seller);

    long countBySeller_Username(String seller);

    List<SellerFollow> findBySeller_Id(Integer sellerId);

    List<SellerFollow> findByFollower_UsernameOrderByCreatedAtDesc(String follower);
}
