package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.LiveShow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LiveShowRepository extends JpaRepository<LiveShow, Integer> {

    // Shows that started in the last day or start later (the list page decides which are still running)
    List<LiveShow> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime after);
}
