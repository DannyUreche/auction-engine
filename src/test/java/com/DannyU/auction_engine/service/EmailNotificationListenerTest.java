package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Ce emailuri se trimit pentru fiecare tip de notificare (fara server de email: doar continutul)
@SpringBootTest(properties = "app.base-url=https://exemplu.ro")
class EmailNotificationListenerTest {

    @Autowired
    private EmailNotificationListener listener;

    @Test
    void ofertaDepasitaTrimiteEmailCuLinkCatreObiect() {
        var email = listener.buildEmail(notification(Notification.Type.OUTBID)).orElseThrow();
        assertThat(email.subject()).startsWith("Oferta ta a fost depasita");
        assertThat(email.text()).contains("Salut, ana!", "Mesajul notificarii", "https://exemplu.ro/items/7");
    }

    @Test
    void castigatSiVandutTrimitEmailDarOfertaNouaNu() {
        assertThat(listener.buildEmail(notification(Notification.Type.WON))).isPresent();
        assertThat(listener.buildEmail(notification(Notification.Type.SOLD))).isPresent();
        assertThat(listener.buildEmail(notification(Notification.Type.NEW_BID))).isEmpty();
        assertThat(listener.buildEmail(notification(Notification.Type.UNSOLD))).isEmpty();
    }

    private Notification notification(Notification.Type type) {
        User user = new User();
        user.setUsername("ana");
        user.setEmail("ana@test.ro");
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setItemId(7);
        notification.setMessage("Mesajul notificarii");
        return notification;
    }
}
