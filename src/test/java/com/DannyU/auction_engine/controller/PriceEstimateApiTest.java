package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.PriceEstimateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// "Cat valoreaza?": estimate from similar sold items
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PriceEstimateApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void similarSalesGiveTheRange() throws Exception {
        String brand = "zorki" + UUID.randomUUID().toString().substring(0, 6);
        User seller = user("vanzator");
        User buyer = user("cumparator");
        for (String price : List.of("100", "200", "300", "400")) {
            Item sold = item(seller, brand + " " + price, price);
            sold.setActive(false);
            sold.setWinner(buyer);
            sold.setEndTime(LocalDateTime.now().minusHours(Integer.parseInt(price) / 100));
            itemRepository.save(sold);
        }
        Item current = item(seller, brand + " model nou", "150");

        mockMvc.perform(get("/api/items/" + current.getId() + "/price-estimate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basis").value("SIMILAR"))
                .andExpect(jsonPath("$.sales").value(4))
                .andExpect(jsonPath("$.low").value(175))
                .andExpect(jsonPath("$.median").value(250))
                .andExpect(jsonPath("$.high").value(325))
                .andExpect(jsonPath("$.recent[0].price").value(100))
                .andExpect(jsonPath("$.recent.length()").value(4));
    }

    @Test
    void percentilesAndWords() {
        List<BigDecimal> prices = List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"));
        assertThat(PriceEstimateService.percentile(prices, 0.5)).isEqualByComparingTo("20");
        assertThat(PriceEstimateService.percentile(prices, 0.25)).isEqualByComparingTo("15");
        assertThat(PriceEstimateService.words("Ceas Omega Seamaster, stare foarte buna")).containsExactlyInAnyOrder("ceas", "omega", "seamaster");
    }

    private User user(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }

    private Item item(User seller, String name, String price) {
        Item item = new Item();
        item.setName(name);
        item.setStartPrice(new BigDecimal(price));
        item.setCurrentPrice(new BigDecimal(price));
        item.setSeller(seller);
        item.setCategory(Category.ELECTRONICE);
        item.setEndTime(LocalDateTime.now().plusDays(1));
        return itemRepository.save(item);
    }
}
