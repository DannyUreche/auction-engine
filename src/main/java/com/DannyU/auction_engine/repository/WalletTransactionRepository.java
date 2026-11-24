package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {

    List<WalletTransaction> findTop30ByUserUsernameOrderByCreatedAtDescIdDesc(String username);
}
