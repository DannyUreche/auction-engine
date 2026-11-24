package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.AppSetting;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.PushSubscription;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.AppSettingRepository;
import com.DannyU.auction_engine.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

// Web Push: every notification (bell) is also sent to the devices where the user turned on push
// notifications, so it arrives on the phone even when the site is closed.
// The VAPID key pair (identifies this server to the push services) is generated once and kept in the DB.
@Slf4j
@Service
public class PushNotificationService {

    private static final String PUBLIC_KEY = "vapid.public";
    private static final String PRIVATE_KEY = "vapid.private";

    private final PushSubscriptionRepository subscriptionRepository;
    private final AppSettingRepository settingRepository;
    private nl.martijndwars.webpush.PushService pushService;
    private String publicKey;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository, AppSettingRepository settingRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.settingRepository = settingRepository;
    }

    @PostConstruct
    void init() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        publicKey = settingRepository.findById(PUBLIC_KEY).map(AppSetting::getValue).orElse(null);
        String privateKey = settingRepository.findById(PRIVATE_KEY).map(AppSetting::getValue).orElse(null);
        if (publicKey == null || privateKey == null) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair pair = generator.generateKeyPair();
            ECPublicKey pub = (ECPublicKey) pair.getPublic();
            byte[] raw = new byte[65];
            raw[0] = 0x04; // uncompressed point: 0x04 | X | Y
            System.arraycopy(unsigned32(pub.getW().getAffineX()), 0, raw, 1, 32);
            System.arraycopy(unsigned32(pub.getW().getAffineY()), 0, raw, 33, 32);
            publicKey = base64Url(raw);
            privateKey = base64Url(unsigned32(((ECPrivateKey) pair.getPrivate()).getS()));
            save(PUBLIC_KEY, publicKey);
            save(PRIVATE_KEY, privateKey);
        }
        pushService = new nl.martijndwars.webpush.PushService(publicKey, privateKey, "mailto:no-reply@licitatii.live");
    }

    public String publicKey() {
        return publicKey;
    }

    @Transactional
    public void subscribe(User user, String endpoint, String p256dh, String auth) {
        PushSubscription subscription = subscriptionRepository.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        subscription.setUser(user);
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        subscriptionRepository.findByEndpoint(endpoint).ifPresent(subscriptionRepository::delete);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(Notification notification) {
        String url = notification.getItemId() == null ? "/account" : "/items/" + notification.getItemId();
        String payload = "{\"title\":\"Licitatii.live\",\"body\":" + json(notification.getMessage()) + ",\"url\":" + json(url) + "}";
        for (PushSubscription subscription : subscriptionRepository.findByUserId(notification.getUser().getId())) {
            try {
                var response = pushService.send(new nl.martijndwars.webpush.Notification(subscription.getEndpoint(),
                        subscription.getP256dh(), subscription.getAuth(), payload.getBytes(StandardCharsets.UTF_8)));
                int status = response.getStatusLine().getStatusCode();
                // The browser unsubscribed (or the subscription expired): forget it
                if (status == 404 || status == 410) {
                    subscriptionRepository.delete(subscription);
                }
            } catch (Exception e) {
                log.warn("Push notification to {} failed: {}", notification.getUser().getUsername(), e.getMessage());
            }
        }
    }

    private void save(String key, String value) {
        AppSetting setting = new AppSetting();
        setting.setKey(key);
        setting.setValue(value);
        settingRepository.save(setting);
    }

    // Big-endian, exactly 32 bytes (BigInteger may add a sign byte or be shorter)
    private static byte[] unsigned32(BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] result = new byte[32];
        int length = Math.min(bytes.length, 32);
        System.arraycopy(bytes, bytes.length - length, result, 32 - length, length);
        return result;
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String json(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                default -> out.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return out.append('"').toString();
    }
}
