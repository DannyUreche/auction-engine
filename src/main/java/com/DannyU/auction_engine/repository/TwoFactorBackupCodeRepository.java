package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.TwoFactorBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TwoFactorBackupCodeRepository extends JpaRepository<TwoFactorBackupCode, Integer> {

    Optional<TwoFactorBackupCode> findFirstByUser_IdAndCodeHashAndUsedFalse(Integer userId, String codeHash);

    long countByUser_IdAndUsedFalse(Integer userId);

    @Modifying
    @Query("DELETE FROM TwoFactorBackupCode c WHERE c.user.id = :userId")
    void deleteForUser(@Param("userId") Integer userId);
}
