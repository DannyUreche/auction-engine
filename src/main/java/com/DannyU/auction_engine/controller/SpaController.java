package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

// In productie interfata React este servita de Spring Boot (din static/index.html).
// Paginile React (ex: /items/5) nu exista pe server, asa ca le trimitem tot la index.html,
// iar React Router afiseaza pagina potrivita. Fara asta, un refresh pe /items/5 ar da 404.
@Controller
public class SpaController {

    private final ItemRepository itemRepository;
    private final String baseUrl;
    private String indexHtml;

    public SpaController(ItemRepository itemRepository, @Value("${app.base-url}") String baseUrl) {
        this.itemRepository = itemRepository;
        this.baseUrl = baseUrl;
    }

    @GetMapping({"/login", "/register", "/forgot-password", "/reset-password", "/verify-email", "/sell", "/account", "/admin",
            "/users/{username}", "/despre", "/confidentialitate", "/termeni", "/clasament"})
    public String forwardToReact() {
        return "forward:/index.html";
    }

    // Item pages get link-preview tags (Open Graph), so a shared link shows the photo, name and price
    // in WhatsApp, Facebook, Messenger... The page itself is still the React app.
    @GetMapping("/items/{id}")
    public ResponseEntity<String> itemPage(@PathVariable String id) {
        String html = indexHtml();
        if (html == null) {
            return ResponseEntity.notFound().build(); // local development: Vite serves the pages
        }
        Item item = id.matches("\\d{1,9}") ? itemRepository.findById(Integer.valueOf(id)).orElse(null) : null;
        String page = item == null ? html : html.replace("</head>", previewTags(item) + "</head>");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/html;charset=UTF-8")).body(page);
    }

    private String previewTags(Item item) {
        String price = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ro-RO")).format(item.getCurrentPrice());
        String description = item.getDescription() == null || item.getDescription().isBlank() ? "" : " · " + item.getDescription().trim();
        if (description.length() > 160) {
            description = description.substring(0, 160) + "...";
        }
        String image = baseUrl + (item.getImageUrl() != null ? item.getImageUrl() : "/icon-512.png");
        String url = baseUrl + "/items/" + item.getId();
        String title = item.getName() + " · Licitatii.live";
        return meta("og:type", "website") + meta("og:site_name", "Licitatii.live") + meta("og:title", title)
                + meta("og:description", (item.getActive() ? "Pret curent: " : "Pret final: ") + price + description)
                + meta("og:image", image) + meta("og:url", url)
                + "<meta name=\"twitter:card\" content=\"summary_large_image\">";
    }

    private static String meta(String property, String content) {
        return "<meta property=\"" + property + "\" content=\"" + escape(content) + "\">";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // static/index.html exists only in the production build (see Dockerfile); read once
    private synchronized String indexHtml() {
        if (indexHtml == null) {
            ClassPathResource resource = new ClassPathResource("static/index.html");
            if (!resource.exists()) {
                return null;
            }
            try {
                indexHtml = resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return indexHtml;
    }
}
