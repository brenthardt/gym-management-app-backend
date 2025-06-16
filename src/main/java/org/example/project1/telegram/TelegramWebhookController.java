package org.example.project1.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramBot telegramBot;

    @PostMapping("/api/webhook")
    public ResponseEntity<BotApiMethod<?>> onUpdateReceived(@RequestBody Update update) {
        try {
            log.info("Received webhook update: {}", update.getUpdateId());
            BotApiMethod<?> response = telegramBot.onWebhookUpdateReceived(update);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing webhook update", e);
            return ResponseEntity.ok().build();
        }
    }
}