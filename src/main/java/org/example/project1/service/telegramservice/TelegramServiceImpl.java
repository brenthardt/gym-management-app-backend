package org.example.project1.service.telegramservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

@Service
public class TelegramServiceImpl implements TelegramService {
    @Value("${telegram.bot.token}")
    private String token;
    RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(SendMessage message) {
        HttpEntity<SendMessage> request = new HttpEntity<>(message);
        restTemplate.postForObject("https://api.telegram.org/bot" + token + "/sendMessage", request, SendMessage.class);
    }

    public void editMessageText(EditMessageText editMessageText) {
        HttpEntity<EditMessageText> request = new HttpEntity<>(editMessageText);
        restTemplate.postForObject("https://api.telegram.org/bot" + token + "/editMessageText", request, EditMessageText.class);
    }

    public void answerInlineQuery(AnswerInlineQuery answerInlineQuery) {
        HttpEntity<AnswerInlineQuery> request = new HttpEntity<>(answerInlineQuery);
        restTemplate.postForObject("https://api.telegram.org/bot" + token + "/answerInlineQuery", request, AnswerInlineQuery.class);
    }

    public void deleteMessage(String chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
        HttpEntity<DeleteMessage> request = new HttpEntity<>(deleteMessage);
        restTemplate.postForObject("https://api.telegram.org/bot" + token + "/deleteMessage", request, DeleteMessage.class);
    }

    public void deleteMessage(DeleteMessage deleteMessage) {
        try {
            HttpEntity<DeleteMessage> request = new HttpEntity<>(deleteMessage);
            restTemplate.postForObject("https://api.telegram.org/bot" + token + "/deleteMessage", request, DeleteMessage.class);
        } catch (Exception ignored) {
        }
    }
}