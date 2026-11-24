package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Integer> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
