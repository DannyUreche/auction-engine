package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// Topping up the wallet with a card through Stripe Checkout. The browser is sent to Stripe; when it comes back
// (and when Stripe's webhook arrives) the session is read from Stripe itself and credited exactly once.
@Service
@RequiredArgsConstructor
public class CardTopUpService {

    static final long MIN_RON = 10;
    static final long MAX_RON = 5000;

    private final StripeGateway stripe;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    public record CardConfig(boolean enabled, boolean testMode, long min, long max) {
    }

    public record Checkout(String url) {
    }

    public record Result(boolean credited, BigDecimal amount) {
    }

    public CardConfig config() {
        return new CardConfig(stripe.isEnabled(), stripe.isTestMode(), MIN_RON, MAX_RON);
    }

    public Checkout start(String username, Long amountRon) {
        if (amountRon == null || amountRon < MIN_RON || amountRon > MAX_RON) {
            throw new IllegalArgumentException("Poti plati cu cardul intre " + MIN_RON + " si " + MAX_RON + " RON odata.");
        }
        String back = baseUrl.replaceAll("/$", "") + "/account?tab=wallet";
        StripeGateway.Session session = stripe.createCheckout(username, amountRon, back + "&stripe_session={CHECKOUT_SESSION_ID}", back + "&stripe=cancel");
        return new Checkout(session.url());
    }

    // The browser came back from Stripe with the session id
    @Transactional
    public Result confirm(String username, String sessionId) {
        StripeGateway.Session session = stripe.retrieve(sessionId);
        if (!username.equals(session.username())) {
            throw new IllegalArgumentException("Aceasta plata nu iti apartine.");
        }
        return credit(session);
    }

    // Stripe's webhook (signed): same crediting, for the case the browser never came back
    @Transactional
    public void webhook(String payload, String signature) {
        stripe.completedSessionId(payload, signature).map(stripe::retrieve).ifPresent(this::credit);
    }

    private Result credit(StripeGateway.Session session) {
        if (!session.paid()) {
            throw new IllegalArgumentException("Plata nu a fost finalizata.");
        }
        if (!"ron".equalsIgnoreCase(session.currency()) || session.amountMinor() <= 0) {
            throw new IllegalArgumentException("Plata are o suma sau o moneda neasteptata.");
        }
        User user = userRepository.findByUsername(session.username())
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul platii nu a fost gasit."));
        BigDecimal amount = BigDecimal.valueOf(session.amountMinor()).movePointLeft(2);
        // One row per session: if the redirect and the webhook arrive together only one of them inserts
        int inserted = entityManager.createNativeQuery("""
                        INSERT INTO stripe_payments (session_id, user_id, amount) VALUES (:session, :user, :amount)
                        ON CONFLICT (session_id) DO NOTHING""")
                .setParameter("session", session.id())
                .setParameter("user", user.getId())
                .setParameter("amount", amount)
                .executeUpdate();
        if (inserted == 0) {
            return new Result(false, amount);
        }
        walletService.credit(user, amount, "Alimentare cu cardul (Stripe" + (stripe.isTestMode() ? ", mod test" : "") + ")");
        return new Result(true, amount);
    }
}
