package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Personal lists: create, add items (idempotent), share publicly, keep private ones private
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CollectionApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void listsCanBeFilledSharedAndHidden() throws Exception {
        User owner = user("colectionar");
        User other = user("vizitator");
        String token = bearer(owner);
        Item watch = item(owner, "Ceas de buzunar", "300");
        Item lamp = item(owner, "Lampa veche", "200");

        String created = mockMvc.perform(post("/api/collections").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"  Cadouri  \", \"description\": \"pentru familie\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cadouri"))
                .andExpect(jsonPath("$.publicList").value(true))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        for (Item item : new Item[]{watch, lamp, watch}) {
            mockMvc.perform(put("/api/collections/" + id + "/items/" + item.getId()).header("Authorization", token))
                    .andExpect(status().isNoContent());
        }
        mockMvc.perform(get("/api/collections/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalValue").value(500.0))
                .andExpect(jsonPath("$.mine").value(false))
                .andExpect(jsonPath("$.owner").value(owner.getUsername()));
        mockMvc.perform(get("/api/collections/containing/" + lamp.getId()).header("Authorization", token))
                .andExpect(jsonPath("$[0]").value(id));
        mockMvc.perform(get("/api/users/" + owner.getUsername() + "/collections"))
                .andExpect(jsonPath("$[0].items").value(2));

        // Strangers can't change it
        mockMvc.perform(delete("/api/collections/" + id + "/items/" + lamp.getId()).header("Authorization", bearer(other)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/collections/" + id + "/items/" + lamp.getId()))
                .andExpect(status().isUnauthorized());

        // Private: only the owner sees it
        mockMvc.perform(put("/api/collections/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Cadouri\", \"publicList\": false}"))
                .andExpect(jsonPath("$.publicList").value(false));
        mockMvc.perform(get("/api/collections/" + id)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/collections/" + id).header("Authorization", bearer(other))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/collections/" + id).header("Authorization", token))
                .andExpect(jsonPath("$.mine").value(true));
        mockMvc.perform(get("/api/users/" + owner.getUsername() + "/collections")).andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(delete("/api/collections/" + id + "/items/" + lamp.getId()).header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/collections/mine").header("Authorization", token))
                .andExpect(jsonPath("$[0].items").value(1));
        mockMvc.perform(post("/api/collections").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/collections/" + id).header("Authorization", token)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/collections/mine").header("Authorization", token)).andExpect(jsonPath("$.length()").value(0));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
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
        item.setEndTime(LocalDateTime.now().plusDays(1));
        return itemRepository.save(item);
    }
}
