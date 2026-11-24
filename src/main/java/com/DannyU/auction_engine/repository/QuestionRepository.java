package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByItemIdOrderByCreatedAtAscIdAsc(Integer itemId);
}
