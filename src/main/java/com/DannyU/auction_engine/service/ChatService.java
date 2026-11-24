package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Message;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Chat-ul dintre castigator si vanzator, dupa vanzare. Nimeni altcineva nu vede mesajele.
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    public List<Message> messages(Integer itemId, String username) {
        checkParticipant(itemId, username);
        return messageRepository.findByItemIdOrderByCreatedAtAscIdAsc(itemId);
    }

    @Transactional
    public Message send(Integer itemId, String username, String text) {
        Item item = checkParticipant(itemId, username);
        boolean fromSeller = item.getSeller().getUsername().equals(username);
        User sender = fromSeller ? item.getSeller() : item.getWinner();
        User recipient = fromSeller ? item.getWinner() : item.getSeller();

        Message message = new Message();
        message.setItem(item);
        message.setSender(sender);
        message.setText(text.trim());
        Message saved = messageRepository.save(message);

        // Browserul celuilalt isi reincarca mesajele (evenimentul nu contine textul)
        eventPublisher.publishEvent(AuctionEvent.message(itemId, recipient.getUsername()));
        String preview = saved.getText().length() > 60 ? saved.getText().substring(0, 60) + "..." : saved.getText();
        notificationService.notify(recipient, Notification.Type.MESSAGE, item,
                "Mesaj nou de la " + sender.getUsername() + " despre \"" + item.getName() + "\": " + preview);
        return saved;
    }

    private Item checkParticipant(Integer itemId, String username) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        if (item.getActive() || item.getWinner() == null) {
            throw new IllegalArgumentException("Chat-ul este disponibil doar dupa ce obiectul a fost vandut!");
        }
        if (!item.getSeller().getUsername().equals(username) && !item.getWinner().getUsername().equals(username)) {
            throw new IllegalArgumentException("Doar vanzatorul si castigatorul pot vedea acest chat!");
        }
        return item;
    }
}
