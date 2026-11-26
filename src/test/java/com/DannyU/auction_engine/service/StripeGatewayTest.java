package com.DannyU.auction_engine.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Stripe webhook signatures: HMAC-SHA256 of "<timestamp>.<payload>", accepted within 5 minutes
class StripeGatewayTest {

    private static final String SECRET = "whsec_test_secret";
    private static final String PAYLOAD = "{\"id\": \"evt_1\", \"type\": \"checkout.session.completed\", \"data\": {\"object\": {\"id\": \"cs_test_abc123\"}}}";

    @Test
    void acceptsOnlyFreshCorrectSignatures() {
        long now = 1_800_000_000L;
        String signature = StripeGateway.hmac(SECRET, now + "." + PAYLOAD);
        assertThat(StripeGateway.validSignature(PAYLOAD, "t=" + now + ",v1=" + signature, SECRET, now + 10)).isTrue();
        assertThat(StripeGateway.validSignature(PAYLOAD, "t=" + now + ",v1=bad,v1=" + signature, SECRET, now)).isTrue();
        assertThat(StripeGateway.validSignature(PAYLOAD + " ", "t=" + now + ",v1=" + signature, SECRET, now)).isFalse();
        assertThat(StripeGateway.validSignature(PAYLOAD, "t=" + now + ",v1=" + signature, "whsec_other", now)).isFalse();
        assertThat(StripeGateway.validSignature(PAYLOAD, "t=" + now + ",v1=" + signature, SECRET, now + 301)).isFalse();
        assertThat(StripeGateway.validSignature(PAYLOAD, null, SECRET, now)).isFalse();
    }

    @Test
    void readsTheSessionOfACompletedCheckout() {
        StripeGateway gateway = new StripeGateway("sk_test_x", SECRET);
        long now = System.currentTimeMillis() / 1000;
        String header = "t=" + now + ",v1=" + StripeGateway.hmac(SECRET, now + "." + PAYLOAD);
        assertThat(gateway.completedSessionId(PAYLOAD, header)).contains("cs_test_abc123");
        assertThat(gateway.isTestMode()).isTrue();
        assertThatThrownBy(() -> gateway.completedSessionId(PAYLOAD, "t=" + now + ",v1=00")).hasMessageContaining("Semnatura");
        assertThat(new StripeGateway("", "").isEnabled()).isFalse();
    }
}
