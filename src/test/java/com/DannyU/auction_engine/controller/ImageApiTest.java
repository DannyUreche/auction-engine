package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste pentru incarcarea si afisarea imaginilor. Imaginile de test se salveaza in target/test-uploads.
@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@AutoConfigureMockMvc
@Transactional
class ImageApiTest {

    private static final Path UPLOAD_DIR = Path.of("target/test-uploads");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void vanzatorulIncarcaImagineaSiOricineOPoateVedea() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);
        byte[] png = pngBytes();

        String imageUrl = upload(item, seller, new MockMultipartFile("image", "poza.png", "image/png", png))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.imageFilename").doesNotExist())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"imageUrl\":\"([^\"]+)\".*", "$1");

        assertThat(imageUrl).matches("/api/images/[0-9a-f-]{36}\\.png");

        // Imaginea se poate descarca fara token (browserul nu trimite token-ul pentru <img>)
        mockMvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(png));
    }

    @Test
    void tipulImaginiiSeStabilesteDupaContinutNuDupaExtensie() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        // Fisier PNG trimis cu nume si tip de JPG: e salvat corect ca PNG
        upload(item, seller, new MockMultipartFile("image", "poza.jpg", "image/jpeg", pngBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", org.hamcrest.Matchers.endsWith(".png")));
    }

    @Test
    void imagineaNouaOInlocuiestePeCeaVeche() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        upload(item, seller, new MockMultipartFile("image", "a.png", "image/png", pngBytes())).andExpect(status().isOk());
        String primul = item.getImageFilename();
        upload(item, seller, new MockMultipartFile("image", "b.png", "image/png", pngBytes())).andExpect(status().isOk());

        assertThat(item.getImageFilename()).isNotEqualTo(primul);
        assertThat(Files.exists(UPLOAD_DIR.resolve(primul))).isFalse();
        assertThat(Files.exists(UPLOAD_DIR.resolve(item.getImageFilename()))).isTrue();
    }

    @Test
    void altUtilizatorNuPoateSchimbaImaginea() throws Exception {
        User seller = createUser("vanzator");
        User altul = createUser("altul");
        Item item = createItem(seller);

        upload(item, altul, new MockMultipartFile("image", "poza.png", "image/png", pngBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Doar vanzatorul poate schimba imaginea obiectului!"));
    }

    @Test
    void fisierulCareNuEImagineEsteRespins() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        upload(item, seller, new MockMultipartFile("image", "poza.png", "image/png", "<html>nu sunt imagine</html>".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Fisierul trebuie sa fie o imagine JPG, PNG, WebP sau GIF!"));
    }

    @Test
    void cerereaFaraFisierEsteRespinsa() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        mockMvc.perform(multipart("/api/items/" + item.getId() + "/image").header("Authorization", bearer(seller)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Imaginea este obligatorie!"));
    }

    @Test
    void incarcareaFaraTokenEsteRespinsa() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        mockMvc.perform(multipart("/api/items/" + item.getId() + "/image")
                        .file(new MockMultipartFile("image", "poza.png", "image/png", pngBytes())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void numeDeFisierInvalideDau404() throws Exception {
        mockMvc.perform(get("/api/images/nu-exista.png")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/images/" + UUID.randomUUID() + ".png")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/images/..%2F..%2Fpom.xml")).andExpect(status().is4xxClientError());
    }

    // ---------- Metode ajutatoare ----------

    private ResultActions upload(Item item, User user, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/items/" + item.getId() + "/image")
                .file(file)
                .header("Authorization", bearer(user)));
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x4F46E5);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
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
