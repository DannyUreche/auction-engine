package com.DannyU.auction_engine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// The few Stripe calls the wallet needs, over plain HTTPS (no SDK): create a Checkout Session, read it back,
// and check webhook signatures. Enabled only with STRIPE_SECRET_KEY; test keys (sk_test_...) never move money.
@Component
public class StripeGateway {

    static final long SIGNATURE_TOLERANCE_SECONDS = 300;
    private static final Pattern SESSION_ID = Pattern.compile("\"id\"\\s*:\\s*\"(cs_[A-Za-z0-9_]+)\"");

    private final String secretKey;
    private final String webhookSecret;
    private final RestClient restClient = RestClient.create("https://api.stripe.com/v1");

    // paid = payment_status "paid"; amountMinor in bani (1 RON = 100)
    public record Session(String id, String url, boolean paid, long amountMinor, String currency, String username) {
    }

    public StripeGateway(@Value("${app.stripe.secret-key:}") String secretKey, @Value("${app.stripe.webhook-secret:}") String webhookSecret) {
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    public boolean isEnabled() {
        return !secretKey.isEmpty();
    }

    public boolean isTestMode() {
        return secretKey.startsWith("sk_test_");
    }

    public Session createCheckout(String username, long amountRon, String successUrl, String cancelUrl) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("success_url", successUrl);
        form.add("cancel_url", cancelUrl);
        form.add("client_reference_id", username);
        form.add("metadata[username]", username);
        form.add("line_items[0][quantity]", "1");
        form.add("line_items[0][price_data][currency]", "ron");
        form.add("line_items[0][price_data][unit_amount]", String.valueOf(amountRon * 100));
        form.add("line_items[0][price_data][product_data][name]", "Alimentare portofel Licitatii.live");
        return call(() -> restClient.post().uri("/checkout/sessions")
                .header("Authorization", "Bearer " + secretKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(Map.class));
    }

    public Session retrieve(String sessionId) {
        if (sessionId == null || !sessionId.matches("cs_[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Plata nu a fost gasita.");
        }
        return call(() -> restClient.get().uri("/checkout/sessions/{id}", sessionId)
                .header("Authorization", "Bearer " + secretKey)
                .retrieve().body(Map.class));
    }

    // The Checkout Session id of a signed "checkout.session.completed" event; empty for other events
    public Optional<String> completedSessionId(String payload, String signatureHeader) {
        if (webhookSecret.isEmpty() || !validSignature(payload, signatureHeader, webhookSecret, System.currentTimeMillis() / 1000)) {
            throw new IllegalArgumentException("Semnatura Stripe invalida.");
        }
        if (!payload.contains("\"checkout.session.completed\"")) {
            return Optional.empty();
        }
        Matcher matcher = SESSION_ID.matcher(payload);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    // Stripe-Signature: "t=<unix time>,v1=<hex HMAC-SHA256 of '<t>.<payload>'>[,v1=...]"
    static boolean validSignature(String payload, String header, String secret, long nowSeconds) {
        if (header == null || payload == null) {
            return false;
        }
        Long timestamp = null;
        java.util.List<String> signatures = new java.util.ArrayList<>();
        for (String part : header.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if (pair[0].equals("t")) {
                try {
                    timestamp = Long.parseLong(pair[1]);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (pair[0].equals("v1")) {
                signatures.add(pair[1]);
            }
        }
        if (timestamp == null || Math.abs(nowSeconds - timestamp) > SIGNATURE_TOLERANCE_SECONDS) {
            return false;
        }
        byte[] expected = hmac(secret, timestamp + "." + payload).getBytes(StandardCharsets.UTF_8);
        return signatures.stream().anyMatch(signature -> MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8)));
    }

    static String hmac(String secret, String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private Session call(java.util.function.Supplier<Map<?, ?>> request) {
        if (!isEnabled()) {
            throw new IllegalArgumentException("Plata cu cardul nu este activata pe acest server.");
        }
        Map<?, ?> body;
        try {
            body = request.get();
        } catch (RestClientException e) {
            throw new IllegalArgumentException("Stripe nu raspunde momentan. Incearca din nou.");
        }
        if (body == null) {
            throw new IllegalArgumentException("Stripe nu a trimis un raspuns.");
        }
        Object metadata = body.get("metadata");
        String username = metadata instanceof Map<?, ?> map && map.get("username") != null ? String.valueOf(map.get("username")) : null;
        Object amount = body.get("amount_total");
        return new Session(String.valueOf(body.get("id")), body.get("url") == null ? null : String.valueOf(body.get("url")),
                "paid".equals(body.get("payment_status")), amount instanceof Number number ? number.longValue() : 0,
                String.valueOf(body.get("currency")), username);
    }
}
