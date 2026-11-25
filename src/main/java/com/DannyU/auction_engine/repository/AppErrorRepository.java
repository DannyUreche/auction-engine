package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.AppError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AppErrorRepository extends JpaRepository<AppError, Integer> {

    Optional<AppError> findFirstByFingerprintAndResolvedFalse(String fingerprint);

    long countByResolvedFalse();

    List<AppError> findTop100ByOrderByResolvedAscLastSeenDesc();

    @Modifying
    @Query("DELETE FROM AppError e WHERE e.resolved = true")
    int deleteResolved();
}
