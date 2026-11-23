package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // Gaseste toate obiectele care inca sunt active, DAR timpul lor a expirat
    List<Item> findByActiveTrueAndEndTimeBefore(LocalDateTime time);

    // Licitatiile deschise, cele care se termina mai repede primele
    List<Item> findByActiveTrueOrderByEndTimeAsc();

    // Obiectele puse la licitatie de un utilizator, cele mai noi primele
    List<Item> findBySellerUsernameOrderByIdDesc(String username);

    // Obiectele castigate de un utilizator, cele mai recente primele
    List<Item> findByWinnerUsernameOrderByEndTimeDesc(String username);
}