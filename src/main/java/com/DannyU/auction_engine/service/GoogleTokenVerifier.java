package com.DannyU.auction_engine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Set;

// Checks a "Sign in with Google" ID token with Google's tokeninfo endpoint: it must be issued by Google,
// for our client id, with a verified email address.
@Component
public class GoogleTokenVerifier {

    public record GoogleProfile(String email, String name) {
    }

    private final String clientId;
    private final RestClient restClient = RestClient.create("https://oauth2.googleapis.com");

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    public boolean isEnabled() {
        return !clientId.isBlank();
    }

    public String clientId() {
        return isEnabled() ? clientId : null;
    }

    public GoogleProfile verify(String credential) {
        Map<?, ?> info;
        try {
            info = restClient.get().uri(uri -> uri.path("/tokeninfo").queryParam("id_token", credential).build())
                    .retrieve().body(Map.class);
        } catch (RestClientException e) {
            throw new IllegalArgumentException("Autentificarea cu Google a esuat!");
        }
        boolean valid = info != null
                && clientId.equals(info.get("aud"))
                && Set.of("accounts.google.com", "https://accounts.google.com").contains(String.valueOf(info.get("iss")))
                && "true".equals(String.valueOf(info.get("email_verified")))
                && info.get("email") != null;
        if (!valid) {
            throw new IllegalArgumentException("Autentificarea cu Google a esuat!");
        }
        return new GoogleProfile(String.valueOf(info.get("email")), String.valueOf(info.get("name") == null ? "" : info.get("name")));
    }
}
