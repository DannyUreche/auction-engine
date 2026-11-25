package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Integer> {

    List<AuditEntry> findTop200ByOrderByCreatedAtDescIdDesc();
}
