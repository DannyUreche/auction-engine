package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.AuctionType;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Server-side search: every test searches for a unique word, so other data in the database doesn't matter
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemSearchApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    private String word;
    private User seller;

    @BeforeEach
    void setUp() {
        word = "cautare" + UUID.randomUUID().toString().substring(0, 8);
        seller = new User();
        seller.setUsername("vanzator_" + word);
        seller.setEmail(seller.getUsername() + "@test.ro");
        seller.setPassword("x");
        userRepository.save(seller);
    }

    @Test
    void filtersCountsAndPages() throws Exception {
        item("Ceas " + word, Category.CEASURI_BIJUTERII, "300", null);
        item("Telefon " + word, Category.ELECTRONICE, "900", "1500");
        item("Laptop " + word, Category.ELECTRONICE, "2500", null);
        Item closed = item("Vechi " + word, Category.ELECTRONICE, "50", null);
        closed.setActive(false);
        itemRepository.save(closed);

        mockMvc.perform(get("/api/items/search").param("q", word.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.allCategories").value(3))
                .andExpect(jsonPath("$.categoryCounts.ELECTRONICE").value(2))
                .andExpect(jsonPath("$.categoryCounts.CEASURI_BIJUTERII").value(1));

        // The category filter narrows the list but not the per-category counts
        mockMvc.perform(get("/api/items/search").param("q", word).param("cat", "ELECTRONICE").param("sort", "price-desc"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Laptop " + word))
                .andExpect(jsonPath("$.categoryCounts.CEASURI_BIJUTERII").value(1));

        mockMvc.perform(get("/api/items/search").param("q", word).param("max", "1000").param("sort", "price-asc"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Ceas " + word));

        mockMvc.perform(get("/api/items/search").param("q", word).param("type", "buy"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Telefon " + word));

        mockMvc.perform(get("/api/items/search").param("q", word).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void dutchAndScheduledTypes() throws Exception {
        Item dutch = item("Olandez " + word, Category.ALTELE, "500", null);
        dutch.setAuctionType(AuctionType.DUTCH);
        dutch.setFloorPrice(new BigDecimal("100"));
        dutch.setPriceDrop(new BigDecimal("1"));
        dutch.setStartTime(LocalDateTime.now().minusMinutes(5));
        itemRepository.save(dutch);
        Item later = item("Programat " + word, Category.ALTELE, "200", null);
        later.setStartTime(LocalDateTime.now().plusHours(3));
        itemRepository.save(later);

        mockMvc.perform(get("/api/items/search").param("q", word).param("type", "dutch"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Olandez " + word));
        mockMvc.perform(get("/api/items/search").param("q", word).param("type", "soon"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Programat " + word));
        // % in the text is searched literally, not as a wildcard
        mockMvc.perform(get("/api/items/search").param("q", "%" + word))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void unknownCategoryIsRejected() throws Exception {
        mockMvc.perform(get("/api/items/search").param("cat", "NIMIC"))
                .andExpect(status().isBadRequest());
    }

    private Item item(String name, Category category, String price, String buyNow) {
        Item item = new Item();
        item.setName(name);
        item.setDescription("Descriere");
        item.setCategory(category);
        item.setStartPrice(new BigDecimal(price));
        item.setCurrentPrice(new BigDecimal(price));
        item.setBuyNowPrice(buyNow == null ? null : new BigDecimal(buyNow));
        item.setSeller(seller);
        item.setEndTime(LocalDateTime.now().plusDays(2));
        return itemRepository.save(item);
    }
}
