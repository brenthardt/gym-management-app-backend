package org.example.project1.service.telegramservice;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;


public interface TelegramService {
    void sendMessage(SendMessage message);

    void deleteMessage(DeleteMessage deleteMessage);
}
