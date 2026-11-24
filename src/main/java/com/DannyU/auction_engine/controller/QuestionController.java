package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Notification;
import com.DannyU.auction_engine.entity.Question;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.QuestionRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// Public questions about an item; anyone logged in may ask (except the seller), only the seller answers
@RestController
@RequestMapping("/api/items/{id}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public record TextRequest(
            @NotBlank(message = "Textul nu poate fi gol!")
            @Size(max = 500, message = "Textul poate avea maxim 500 de caractere!")
            String text) {
    }

    @GetMapping
    public List<Question> list(@PathVariable Integer id) {
        return questionRepository.findByItemIdOrderByCreatedAtAscIdAsc(id);
    }

    @PostMapping
    @Transactional
    public Question ask(@PathVariable Integer id, @Valid @RequestBody TextRequest request, Authentication authentication) {
        Item item = findItem(id);
        if (item.getSeller().getUsername().equals(authentication.getName())) {
            throw new IllegalArgumentException("Nu poti pune intrebari la propriul obiect!");
        }
        User author = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
        Question question = new Question();
        question.setItem(item);
        question.setAuthor(author);
        question.setText(request.text().trim());
        Question saved = questionRepository.save(question);
        notificationService.notify(item.getSeller(), Notification.Type.QUESTION, item,
                "Intrebare noua despre \"" + item.getName() + "\": " + shorten(saved.getText()));
        return saved;
    }

    @PostMapping("/{questionId}/answer")
    @Transactional
    public Question answer(@PathVariable Integer id, @PathVariable Integer questionId, @Valid @RequestBody TextRequest request,
                           Authentication authentication) {
        Item item = findItem(id);
        if (!item.getSeller().getUsername().equals(authentication.getName())) {
            throw new IllegalArgumentException("Doar vanzatorul poate raspunde la intrebari!");
        }
        Question question = questionRepository.findById(questionId)
                .filter(q -> q.getItem().getId().equals(id))
                .orElseThrow(() -> new IllegalArgumentException("Intrebarea nu a fost gasita!"));
        if (question.getAnswer() != null) {
            throw new IllegalArgumentException("Ai raspuns deja la aceasta intrebare!");
        }
        question.setAnswer(request.text().trim());
        question.setAnsweredAt(LocalDateTime.now());
        notificationService.notify(question.getAuthor(), Notification.Type.ANSWER, item,
                "Vanzatorul a raspuns la intrebarea ta despre \"" + item.getName() + "\": " + shorten(question.getAnswer()));
        return question;
    }

    private Item findItem(Integer id) {
        return itemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
    }

    private static String shorten(String text) {
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }
}
