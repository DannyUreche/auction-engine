package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.SavedAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedAlertRepository extends JpaRepository<SavedAlert, Integer> {

    List<SavedAlert> findByUserUsernameOrderByCreatedAtDesc(String username);

    long countByUserUsername(String username);
}
