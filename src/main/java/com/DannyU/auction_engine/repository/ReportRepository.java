package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    boolean existsByItemIdAndReporterUsername(Integer itemId, String username);

    List<Report> findByStatusOrderByCreatedAtAsc(Report.Status status);
}
