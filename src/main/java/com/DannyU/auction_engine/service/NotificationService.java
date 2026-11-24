package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Creeaza notificarile si anunta browserul destinatarului (eveniment live "notification").
// Evenimentul live contine doar username-ul destinatarului, nu si textul: browserul isi cere singur lista.
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public record NotificationList(long unread, List<Notification> items) {
    }

    public void notify(User user, Notification.Type type, Item item, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setItemId(item == null ? null : item.getId());
        notification.setMessage(message);
        notificationRepository.save(notification);
        eventPublisher.publishEvent(AuctionEvent.notification(user.getUsername()));
        // Pentru emailuri (vezi EmailNotificationListener)
        eventPublisher.publishEvent(notification);
    }

    public NotificationList list(String username) {
        return new NotificationList(notificationRepository.countByUserUsernameAndReadFalse(username),
                notificationRepository.findTop30ByUserUsernameOrderByCreatedAtDescIdDesc(username));
    }

    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllRead(username);
    }
}
