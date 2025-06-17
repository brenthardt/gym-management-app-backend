package org.example.project1.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.project1.telegram.TelegramBot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RequestMapping("/api/webhook")
@RestController
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramBot bot;

    @PostMapping
    public void onUpdateReceived(@RequestBody Update update) {
        bot.onUpdateReceived(update);
    }
}