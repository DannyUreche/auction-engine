package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste pe rutele HTTP: autentificare (401), validare (400) si fluxul de licitare.
// Fiecare test ruleaza intr-o tranzactie anulata la final, deci nu lasa date in baza.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiTest {

    private static final String MESAJ_401 = "Trebuie sa fii autentificat (token lipsa, invalid sau expirat)!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ---------- Autentificare ----------

    @Test
    void faraTokenPrimesti401PeRuteleProtejate() throws Exception {
        for (String url : new String[]{"/api/items/my", "/api/items/won", "/api/bids/my", "/api/users/me"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.eroare").value(MESAJ_401));
        }
        postJson("/api/items", null, "{\"name\":\"X\",\"startPrice\":10}").andExpect(status().isUnauthorized());
        postJson("/api/bids", null, "{\"itemId\":1,\"amount\":10}").andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFalsificatPrimeste401() throws Exception {
        mockMvc.perform(get("/api/items/my").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.eroare").value(MESAJ_401));
    }

    @Test
    void licitatiileSuntVizibileFaraCont() throws Exception {
        User seller = createUser("public");
        User bidder = createUser("ofertant");
        Item item = createItem(seller);
        postJson("/api/bids", bearer(bidder), """
                {"itemId":%d,"amount":150}
                """.formatted(item.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/items")).andExpect(status().isOk());
        mockMvc.perform(get("/api/items/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Obiect test"))
                .andExpect(jsonPath("$.seller.email").doesNotExist());
        mockMvc.perform(get("/api/bids/" + item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(150))
                .andExpect(jsonPath("$[0].bidder.email").doesNotExist());
    }

    @Test
    void tokenExpiratPrimeste401() throws Exception {
        User user = createUser("expirat");
        // Un JwtService cu durata negativa genereaza token-uri deja expirate
        String tokenExpirat = new JwtService(jwtSecret, -60_000).generateToken(user.getUsername());

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokenExpirat))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.eroare").value(MESAJ_401));
    }

    @Test
    void tokenValidDaAccesLaProfil() throws Exception {
        User user = createUser("profil");

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void ruteleVechiDeUtilizatoriNuMaiExista() throws Exception {
        User user = createUser("vechi");

        mockMvc.perform(get("/api/users").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void inregistrareSiLoginFunctioneaza() throws Exception {
        String username = "nou_" + UUID.randomUUID().toString().substring(0, 8);

        postJson("/api/auth/register", null, """
                {"username":"%s","email":"%s@test.ro","password":"parola123"}
                """.formatted(username, username))
                .andExpect(status().isOk());

        postJson("/api/auth/login", null, """
                {"username":"%s","password":"parola123"}
                """.formatted(username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginCuParolaGresitaEsteRespins() throws Exception {
        User user = createUser("login");

        postJson("/api/auth/login", null, """
                {"username":"%s","password":"gresita"}
                """.formatted(user.getUsername()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Username sau parola incorecta!"));
    }

    @Test
    void inregistrareCuUsernameExistentEsteRespinsa() throws Exception {
        User user = createUser("dublura");

        postJson("/api/auth/register", null, """
                {"username":"%s","email":"altul_%s@test.ro","password":"parola123"}
                """.formatted(user.getUsername(), UUID.randomUUID().toString().substring(0, 8)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Acest username este deja folosit!"));
    }

    // ---------- Validare ----------

    @Test
    void inregistrareCuDateInvalideEsteRespinsa() throws Exception {
        postJson("/api/auth/register", null, """
                {"username":"ab","email":"nu-e-email","password":"123"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare", containsString("Email-ul nu este valid!")))
                .andExpect(jsonPath("$.eroare", containsString("Parola trebuie sa aiba minim 6 caractere!")))
                .andExpect(jsonPath("$.eroare", containsString("Username-ul trebuie sa aiba intre 3 si 50 de caractere!")));
    }

    @Test
    void obiectCuDateInvalideEsteRespins() throws Exception {
        User seller = createUser("vanzator");

        postJson("/api/items", bearer(seller), """
                {"name":" ","startPrice":-5,"durationInMinutes":0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare", containsString("Numele obiectului este obligatoriu!")))
                .andExpect(jsonPath("$.eroare", containsString("Pretul de pornire trebuie sa fie mai mare decat 0!")))
                .andExpect(jsonPath("$.eroare", containsString("Durata licitatiei trebuie sa fie de cel putin un minut!")));
    }

    @Test
    void ofertaFaraDateEsteRespinsa() throws Exception {
        User bidder = createUser("cumparator");

        postJson("/api/bids", bearer(bidder), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare", containsString("Id-ul obiectului este obligatoriu!")))
                .andExpect(jsonPath("$.eroare", containsString("Suma oferita este obligatorie!")));
    }

    @Test
    void jsonStricatEsteRespins() throws Exception {
        User bidder = createUser("cumparator");

        postJson("/api/bids", bearer(bidder), "{\"itemId\":1,\"amount\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Cererea are un format invalid!"));
    }

    // ---------- Obiecte si oferte ----------

    @Test
    void creareObiectFolosesteUtilizatorulDinToken() throws Exception {
        User seller = createUser("vanzator");

        postJson("/api/items", bearer(seller), """
                {"name":"Ceas vechi","startPrice":100}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seller.username").value(seller.getUsername()))
                .andExpect(jsonPath("$.seller.email").doesNotExist())
                .andExpect(jsonPath("$.currentPrice").value(100))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void obiectInexistentEsteRespins() throws Exception {
        User user = createUser("cautare");

        mockMvc.perform(get("/api/items/-1").header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Obiectul nu a fost gasit!"));
    }

    @Test
    void fluxDeLicitarePrinApi() throws Exception {
        User seller = createUser("vanzator");
        User bidder = createUser("cumparator");
        Item item = createItem(seller);

        // Oferta valida de la alt utilizator
        postJson("/api/bids", bearer(bidder), """
                {"itemId":%d,"amount":150}
                """.formatted(item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidder.username").value(bidder.getUsername()))
                .andExpect(jsonPath("$.bidder.email").doesNotExist())
                .andExpect(jsonPath("$.item.seller.email").doesNotExist())
                .andExpect(jsonPath("$.amount").value(150));

        // Vanzatorul nu poate licita la propriul obiect
        postJson("/api/bids", bearer(seller), """
                {"itemId":%d,"amount":200}
                """.formatted(item.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Nu poti licita pentru propriul obiect!"));

        // Oferta apare in lista mea si in istoricul obiectului
        mockMvc.perform(get("/api/bids/my").header("Authorization", bearer(bidder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(150));

        mockMvc.perform(get("/api/bids/" + item.getId()).header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bidder.username").value(bidder.getUsername()))
                .andExpect(jsonPath("$[0].bidder.email").doesNotExist());

        // Obiectul apare la "obiectele mele" ale vanzatorului
        mockMvc.perform(get("/api/items/my").header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(item.getId()))
                .andExpect(jsonPath("$[0].currentPrice").value(150));
    }

    // ---------- Metode ajutatoare ----------

    private ResultActions postJson(String url, String authHeader, String body) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (authHeader != null) {
            request.header("Authorization", authHeader);
        }
        return mockMvc.perform(request);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User createUser(String prefix) {
        String unic = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(unic);
        user.setEmail(unic + "@test.ro");
        user.setPassword("parola-test");
        return userRepository.save(user);
    }

    private Item createItem(User owner) {
        Item newItem = new Item();
        newItem.setName("Obiect test");
        newItem.setStartPrice(new BigDecimal("100.00"));
        newItem.setCurrentPrice(new BigDecimal("100.00"));
        newItem.setSeller(owner);
        newItem.setEndTime(LocalDateTime.now().plusHours(1));
        return itemRepository.save(newItem);
    }
}
