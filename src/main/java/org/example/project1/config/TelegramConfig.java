package org.example.project1.config;

import lombok.RequiredArgsConstructor;
import org.example.project1.telegram.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
public class TelegramConfig {

    private final TelegramBot telegramBot;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        
        SetWebhook setWebhook = SetWebhook.builder()
                .url(telegramBot.getBotPath())
                .build();
        
        api.registerBot(telegramBot, setWebhook);
        return api;
    }
}