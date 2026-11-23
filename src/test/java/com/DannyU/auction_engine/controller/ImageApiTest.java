package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemImageRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste pentru pozele obiectelor (maxim 5, salvate in baza de date; tranzactia e anulata la final)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImageApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ItemImageRepository itemImageRepository;
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
    void pozeleSeAdaugaInOrdineSiSuntMaxim5() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        for (int i = 0; i < Item.MAX_IMAGES; i++) {
            uploadPng(item, seller).andExpect(status().isOk());
        }
        List<String> poze = item.getImageFilenames();
        assertThat(poze).hasSize(5).doesNotHaveDuplicates();

        // Prima poza adaugata e poza principala (cea de pe card)
        mockMvc.perform(get("/api/items/" + item.getId()))
                .andExpect(jsonPath("$.imageUrls.length()").value(5))
                .andExpect(jsonPath("$.imageUrls[0]").value("/api/images/" + poze.getFirst()))
                .andExpect(jsonPath("$.imageUrl").value("/api/images/" + poze.getFirst()))
                .andExpect(jsonPath("$.imageFilenames").doesNotExist());

        uploadPng(item, seller)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Un obiect poate avea maxim 5 poze!"));
    }

    @Test
    void stergereaUneiPozeOEliminaSiDinBazaDeDate() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);
        uploadPng(item, seller).andExpect(status().isOk());
        uploadPng(item, seller).andExpect(status().isOk());
        String prima = item.getImageFilenames().get(0);
        String aDoua = item.getImageFilenames().get(1);

        mockMvc.perform(delete("/api/items/" + item.getId() + "/images/" + prima).header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrls.length()").value(1))
                .andExpect(jsonPath("$.imageUrl").value("/api/images/" + aDoua));

        assertThat(itemImageRepository.existsById(prima)).isFalse();
        assertThat(itemImageRepository.existsById(aDoua)).isTrue();

        mockMvc.perform(delete("/api/items/" + item.getId() + "/images/" + prima).header("Authorization", bearer(seller)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Poza nu a fost gasita!"));
    }

    @Test
    void oricarePozaPoateDeveniPozaPrincipala() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);
        for (int i = 0; i < 3; i++) {
            uploadPng(item, seller).andExpect(status().isOk());
        }
        List<String> inainte = List.copyOf(item.getImageFilenames());

        mockMvc.perform(post("/api/items/" + item.getId() + "/images/" + inainte.get(2) + "/main")
                        .header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("/api/images/" + inainte.get(2)));

        assertThat(item.getImageFilenames()).containsExactly(inainte.get(2), inainte.get(0), inainte.get(1));
    }

    @Test
    void altUtilizatorNuPoateSchimbaImaginea() throws Exception {
        User seller = createUser("vanzator");
        User altul = createUser("altul");
        Item item = createItem(seller);

        upload(item, altul, new MockMultipartFile("image", "poza.png", "image/png", pngBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Doar vanzatorul poate schimba pozele obiectului!"));

        uploadPng(item, seller).andExpect(status().isOk());
        mockMvc.perform(delete("/api/items/" + item.getId() + "/images/" + item.getImageFilenames().getFirst())
                        .header("Authorization", bearer(altul)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Doar vanzatorul poate schimba pozele obiectului!"));
        assertThat(item.getImageFilenames()).hasSize(1);
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

        mockMvc.perform(multipart("/api/items/" + item.getId() + "/images").header("Authorization", bearer(seller)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Imaginea este obligatorie!"));
    }

    @Test
    void incarcareaFaraTokenEsteRespinsa() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        mockMvc.perform(multipart("/api/items/" + item.getId() + "/images")
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
        return mockMvc.perform(multipart("/api/items/" + item.getId() + "/images")
                .file(file)
                .header("Authorization", bearer(user)));
    }

    private ResultActions uploadPng(Item item, User user) throws Exception {
        return upload(item, user, new MockMultipartFile("image", "poza.png", "image/png", pngBytes()));
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
