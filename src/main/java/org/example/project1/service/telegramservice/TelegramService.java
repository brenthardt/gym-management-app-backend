package org.example.project1.service.telegramservice;

import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

public interface TelegramService {
    void sendMessage(SendMessage sendMessage);
    void editMessageText(EditMessageText editMessageText);
    void answerInlineQuery(AnswerInlineQuery answerInlineQuery);
    void deleteMessage(String chatId, Integer messageId);
} 