package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.ItemImage;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemImageRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

// Date demo pentru prezentari: utilizatori, obiecte cu poze generate, oferte cu istoric si cateva vanzari.
// Utilizatorii demo au username-ul "demo_..." si o parola aleatoare (nu se poate intra in contul lor).
@Service
@RequiredArgsConstructor
public class DemoDataService {

    public static final String PREFIX = "demo_";

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final ItemImageRepository itemImageRepository;
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;

    private record DemoItem(String name, String description, Category category, int startPrice, Integer buyNow,
                            Color from, Color to) {
    }

    private static final String[] USERS = {"ana", "mihai", "ioana", "radu", "elena", "vlad", "bianca"};

    private static final List<DemoItem> ITEMS = List.of(
            new DemoItem("Ceas Omega Seamaster 1968", "Mecanism automatic revizuit anul trecut, cutie originala. Tine perfect ora.",
                    Category.CEASURI_BIJUTERII, 1200, 2600, new Color(0x0f172a), new Color(0xb45309)),
            new DemoItem("Inel cu safir, aur 14k", "Safir natural de 1,2 carate, marimea 54. Certificat de autenticitate inclus.",
                    Category.CEASURI_BIJUTERII, 800, null, new Color(0x1e3a8a), new Color(0x60a5fa)),
            new DemoItem("Tablou in ulei - Peisaj din Bucovina", "Pictura originala pe panza, 60x80 cm, rama din lemn sculptat.",
                    Category.ARTA_COLECTII, 450, null, new Color(0x14532d), new Color(0xfbbf24)),
            new DemoItem("Colectie monede romanesti 1900-1947", "42 de monede in album, inclusiv 5 lei 1930 si 100 lei 1932.",
                    Category.ARTA_COLECTII, 300, 900, new Color(0x78350f), new Color(0xfde68a)),
            new DemoItem("iPhone 14 Pro 256GB", "Baterie 91%, fara zgarieturi, cu husa si incarcator. Liber de retea.",
                    Category.ELECTRONICE, 2500, 3800, new Color(0x312e81), new Color(0xa855f7)),
            new DemoItem("PlayStation 5 + 2 controllere", "Varianta cu unitate optica, 3 jocuri incluse. Folosita foarte putin.",
                    Category.ELECTRONICE, 1500, null, new Color(0x0c4a6e), new Color(0x38bdf8)),
            new DemoItem("Aparat foto Canon AE-1 Program", "Film 35mm, obiectiv 50mm f/1.8. Testat, declanseaza la toate vitezele.",
                    Category.ELECTRONICE, 400, null, new Color(0x18181b), new Color(0x71717a)),
            new DemoItem("Geaca de piele vintage", "Piele naturala, marimea M, anii '80. Captuseala refacuta.",
                    Category.MODA, 250, null, new Color(0x451a03), new Color(0xd97706)),
            new DemoItem("Geanta Michael Kors", "Model Jet Set, culoare bej, purtata de cateva ori. Cu factura.",
                    Category.MODA, 350, 700, new Color(0x831843), new Color(0xf9a8d4)),
            new DemoItem("Set mobilier gradina din lemn masiv", "Masa + 6 scaune din salcam, tratate pentru exterior.",
                    Category.CASA_GRADINA, 900, null, new Color(0x365314), new Color(0xa3e635)),
            new DemoItem("Lampa Art Deco din alama", "Abajur din sticla opalina, functionala, cablu nou.",
                    Category.CASA_GRADINA, 200, null, new Color(0x713f12), new Color(0xfacc15)),
            new DemoItem("Bicicleta de cursiera Peugeot 1985", "Cadru Reynolds 531, schimbator Shimano 600. Restaurata complet.",
                    Category.SPORT, 600, null, new Color(0x7f1d1d), new Color(0xf87171)),
            new DemoItem("Placa snowboard Burton Custom 156", "Cu legaturi Burton Cartel, un sezon folosita.",
                    Category.SPORT, 450, 950, new Color(0x164e63), new Color(0x67e8f9)),
            new DemoItem("Morometii - prima editie, 1955", "Marin Preda, ESPLA 1955. Stare foarte buna pentru varsta.",
                    Category.CARTI_MUZICA, 150, null, new Color(0x44403c), new Color(0xd6d3d1)),
            new DemoItem("Colectie viniluri Phoenix", "Cantafabule, Mugur de fluier si Cei ce ne-au dat nume. Coperti originale.",
                    Category.CARTI_MUZICA, 220, null, new Color(0x1c1917), new Color(0xef4444)),
            new DemoItem("Dacia 1300 restaurata", "An 1978, 62.000 km reali, motor refacut. Acte la zi, inmatriculata.",
                    Category.VEHICULE, 15000, 22000, new Color(0x1e293b), new Color(0x22d3ee)));

    public boolean exists() {
        return userRepository.findByUsername(PREFIX + USERS[0]).isPresent();
    }

    @Transactional
    public int generate() {
        if (exists()) {
            throw new IllegalArgumentException("Datele demo exista deja! Sterge-le inainte sa le generezi din nou.");
        }
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();

        List<User> users = new ArrayList<>();
        for (String name : USERS) {
            User user = new User();
            user.setUsername(PREFIX + name);
            user.setEmail(PREFIX + name + "@demo.licitatii.live");
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setCreatedAt(now.minusDays(10 + random.nextInt(60)));
            users.add(userRepository.save(user));
        }

        // Cat mai e pana la final; ultimele 3 obiecte din lista sunt deja vandute
        Duration[] remaining = {Duration.ofMinutes(4), Duration.ofMinutes(25), Duration.ofHours(2), Duration.ofHours(7),
                Duration.ofDays(1), Duration.ofDays(3)};
        for (int i = 0; i < ITEMS.size(); i++) {
            DemoItem demo = ITEMS.get(i);
            boolean sold = i % 5 == 4;
            User seller = users.get(i % users.size());

            Item item = new Item();
            item.setName(demo.name());
            item.setDescription(demo.description());
            item.setCategory(demo.category());
            item.setStartPrice(BigDecimal.valueOf(demo.startPrice()));
            item.setCurrentPrice(BigDecimal.valueOf(demo.startPrice()));
            item.setBuyNowPrice(demo.buyNow() == null ? null : BigDecimal.valueOf(demo.buyNow()));
            item.setSeller(seller);
            item.setEndTime(sold ? now.minusHours(1 + random.nextInt(20)) : now.plus(remaining[i % remaining.length]));
            int photos = 1 + random.nextInt(3);
            for (int p = 0; p < photos; p++) {
                item.getImageFilenames().add(storeImage(demo, p, random));
            }
            itemRepository.save(item);

            // Oferte de la alti utilizatori, in ordine, de-a lungul ultimelor 2 zile
            int bidCount = sold ? 4 + random.nextInt(4) : random.nextInt(9);
            LocalDateTime time = now.minusHours(48);
            BigDecimal price = item.getStartPrice();
            User last = null;
            for (int b = 0; b < bidCount; b++) {
                User bidder;
                do {
                    bidder = users.get(random.nextInt(users.size()));
                } while (bidder.equals(seller) || bidder.equals(last));
                price = price.add(BidRules.increment(price).multiply(BigDecimal.valueOf(1 + random.nextInt(4))));
                if (item.getBuyNowPrice() != null && price.compareTo(item.getBuyNowPrice()) >= 0) {
                    break;
                }
                time = time.plusMinutes(30 + random.nextInt(sold ? 300 : 500));
                if (time.isAfter(sold ? item.getEndTime() : now)) {
                    break;
                }
                Bid bid = new Bid();
                bid.setItem(item);
                bid.setBidder(bidder);
                bid.setAmount(price);
                bid.setCreatedAt(time);
                bidRepository.save(bid);
                item.setCurrentPrice(price);
                last = bidder;
            }
            if (sold) {
                item.setActive(false);
                item.setWinner(last);
            }
            itemRepository.save(item);
        }
        return ITEMS.size();
    }

    // Sterge tot ce tine de utilizatorii demo (obiecte, oferte, poze) si utilizatorii insisi
    @Transactional
    public int delete() {
        List<User> demoUsers = userRepository.findAll().stream()
                .filter(u -> u.getUsername().startsWith(PREFIX) && u.getEmail().endsWith("@demo.licitatii.live"))
                .toList();
        List<Integer> items = itemRepository.findAll().stream()
                .filter(item -> demoUsers.contains(item.getSeller()))
                .map(Item::getId)
                .toList();
        items.forEach(adminService::deleteItem);
        bidRepository.deleteAll(bidRepository.findAll().stream().filter(b -> demoUsers.contains(b.getBidder())).toList());
        userRepository.deleteAll(demoUsers);
        return items.size();
    }

    // O "poza" abstracta: degrade in culorile obiectului, cu cercuri translucide si numele obiectului
    private String storeImage(DemoItem demo, int index, Random random) {
        int width = 800;
        int height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean flip = index % 2 == 1;
        g.setPaint(new GradientPaint(0, 0, flip ? demo.to() : demo.from(), width, height, flip ? demo.from() : demo.to()));
        g.fillRect(0, 0, width, height);
        for (int c = 0; c < 7; c++) {
            int size = 80 + random.nextInt(320);
            g.setColor(new Color(255, 255, 255, 18 + random.nextInt(40)));
            g.fill(new Ellipse2D.Double(random.nextInt(width) - size / 2.0, random.nextInt(height) - size / 2.0, size, size));
        }
        g.setColor(new Color(255, 255, 255, 230));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        String title = demo.name().length() > 30 ? demo.name().substring(0, 30) + "..." : demo.name();
        g.drawString(title, 40, height - 60);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.setColor(new Color(255, 255, 255, 170));
        g.drawString("Poza " + (index + 1) + " · date demo", 40, height - 28);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ItemImage stored = new ItemImage();
        stored.setFilename(UUID.randomUUID() + ".jpg");
        stored.setContentType("image/jpeg");
        stored.setData(out.toByteArray());
        return itemImageRepository.save(stored).getFilename();
    }
}
