package org.example.project1.service.telegramservice;

import lombok.RequiredArgsConstructor;
import org.example.project1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;

@Service
@RequiredArgsConstructor
public class TelegramServiceImpl implements TelegramService {
    @Value("${telegram.bot.token}")
    private String token;
    
    private final UserRepository userRepository;
    RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(SendMessage message) {
        try {
            HttpEntity<SendMessage> request = new HttpEntity<>(message);
            restTemplate.postForObject("https://api.telegram.org/bot" + token + "/sendMessage", request, SendMessage.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                System.out.println("Bot was blocked by user with chat ID: " + message.getChatId());
                try {
                    Long chatId = Long.parseLong(message.getChatId());
                    userRepository.updateBotBlockedStatus(chatId, Boolean.TRUE);
                    System.out.println("Marked user with chat ID " + message.getChatId() + " as blocked");
                } catch (Exception dbError) {
                    System.out.println("Failed to update blocked status for chat ID " + message.getChatId() + ": " + dbError.getMessage());
                }
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                System.out.println("Bad request when sending message to chat ID: " + message.getChatId() + ". Error: " + e.getMessage());
            } else {
                System.out.println("HTTP error when sending message to chat ID: " + message.getChatId() + ". Status: " + e.getStatusCode() + ", Error: " + e.getMessage());
            }
        } catch (RestClientException e) {
            System.out.println("Rest client error when sending message to chat ID: " + message.getChatId() + ". Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error when sending message to chat ID: " + message.getChatId() + ". Error: " + e.getMessage());
        }
    }

    public void deleteMessage(DeleteMessage deleteMessage) {
        try {
            HttpEntity<DeleteMessage> request = new HttpEntity<>(deleteMessage);
            restTemplate.postForObject("https://api.telegram.org/bot" + token + "/deleteMessage", request, DeleteMessage.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                System.out.println("Bot was blocked by user when trying to delete message in chat ID: " + deleteMessage.getChatId());
                try {
                    Long chatId = Long.parseLong(deleteMessage.getChatId());
                    userRepository.updateBotBlockedStatus(chatId, Boolean.TRUE);
                    System.out.println("Marked user with chat ID " + deleteMessage.getChatId() + " as blocked");
                } catch (Exception dbError) {
                    System.out.println("Failed to update blocked status for chat ID " + deleteMessage.getChatId() + ": " + dbError.getMessage());
                }
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                System.out.println("Bad request when deleting message in chat ID: " + deleteMessage.getChatId() + ". Error: " + e.getMessage());
            } else {
                System.out.println("HTTP error when deleting message in chat ID: " + deleteMessage.getChatId() + ". Status: " + e.getStatusCode() + ", Error: " + e.getMessage());
            }
        } catch (RestClientException e) {
            System.out.println("Rest client error when deleting message in chat ID: " + deleteMessage.getChatId() + ". Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error when deleting message in chat ID: " + deleteMessage.getChatId() + ". Error: " + e.getMessage());
        }
    }
}
