package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.LiveShow;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.LiveShowRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AuctionEvent;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Live show video signaling: only the organizer streams, messages reach only their recipient, at most 8 viewers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class LiveStreamApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LiveShowRepository showRepository;
    @Autowired
    private ApplicationEvents events;

    @Test
    void signalingGoesOnlyToTheRecipient() throws Exception {
        User seller = user("video_v");
        User stranger = user("video_x");
        LiveShow show = new LiveShow();
        show.setSeller(seller);
        show.setTitle("Seara de colectie");
        show.setStartTime(LocalDateTime.now());
        show.setSlotSeconds(120);
        show = showRepository.save(show);
        String base = "/api/shows/" + show.getId() + "/stream";
        String host = "host-" + show.getId();
        String viewer = UUID.randomUUID().toString();

        mockMvc.perform(get(base)).andExpect(jsonPath("$.live").value(false));
        send(post(base + "/join"), null, "{\"peer\": \"" + viewer + "\"}").andExpect(jsonPath("$.eroare").value("Vanzatorul nu transmite video acum."));
        send(post(base + "/start"), stranger, "{}").andExpect(status().isBadRequest());
        send(post(base + "/start"), seller, "{}").andExpect(jsonPath("$.live").value(true));

        // A viewer (no account needed) joins: the host's inbox gets the "join"
        send(post(base + "/join"), null, "{\"peer\": \"" + viewer + "\"}").andExpect(jsonPath("$.viewers").value(1));
        mockMvc.perform(get(base + "/inbox").param("peer", host)).andExpect(status().isBadRequest());
        mockMvc.perform(get(base + "/inbox").param("peer", host).header("Authorization", bearer(seller)))
                .andExpect(jsonPath("$[0].kind").value("join"))
                .andExpect(jsonPath("$[0].from").value(viewer));

        // Only the organizer can speak as the host
        String offer = "{\"from\": \"" + host + "\", \"to\": \"" + viewer + "\", \"kind\": \"offer\", \"data\": \"v=0 sdp\"}";
        send(post(base + "/signal"), stranger, offer).andExpect(status().isBadRequest());
        send(post(base + "/signal"), seller, offer).andExpect(status().isNoContent());
        mockMvc.perform(get(base + "/inbox").param("peer", viewer))
                .andExpect(jsonPath("$[0].kind").value("offer"))
                .andExpect(jsonPath("$[0].data").value("v=0 sdp"));
        mockMvc.perform(get(base + "/inbox").param("peer", viewer)).andExpect(jsonPath("$.length()").value(0));

        // A viewer can only talk to the host
        String other = UUID.randomUUID().toString();
        send(post(base + "/signal"), null, "{\"from\": \"" + viewer + "\", \"to\": \"" + other + "\", \"kind\": \"answer\", \"data\": \"x\"}")
                .andExpect(status().isBadRequest());
        send(post(base + "/signal"), null, "{\"from\": \"" + viewer + "\", \"to\": \"" + host + "\", \"kind\": \"answer\", \"data\": \"v=0 answer\"}")
                .andExpect(status().isNoContent());

        // The public event stream only carries "peer X has mail", never the content
        assertThat(events.stream(AuctionEvent.class).filter(e -> e.type().equals("rtc")))
                .isNotEmpty()
                .allSatisfy(e -> assertThat(e.amount()).isNull());

        // Peer-to-peer: at most 8 viewers
        for (int i = 1; i < 8; i++) {
            send(post(base + "/join"), null, "{\"peer\": \"" + UUID.randomUUID() + "\"}").andExpect(status().isOk());
        }
        send(post(base + "/join"), null, "{\"peer\": \"" + UUID.randomUUID() + "\"}").andExpect(status().isBadRequest());

        send(post(base + "/stop"), seller, "{}").andExpect(status().isNoContent());
        mockMvc.perform(get(base)).andExpect(jsonPath("$.live").value(false));
        assertThat(events.stream(AuctionEvent.class).filter(e -> e.type().equals("stream")).count()).isEqualTo(2);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, User user, String body) throws Exception {
        if (user != null) {
            request.header("Authorization", bearer(user));
        }
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
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
}
