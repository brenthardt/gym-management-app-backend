package org.example.project1.service.telegramservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
    
    private final RestTemplate restTemplate;
    
    public TelegramServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    public void sendMessage(SendMessage message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SendMessage> request = new HttpEntity<>(message, headers);
        restTemplate.postForObject("https://api.telegram.org/bot" + token + "/sendMessage", request, SendMessage.class);
    }

    
    public void editMessageText(EditMessageText editMessageText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EditMessageText> request = new HttpEntity<>(editMessageText, headers);
        restTemplate.postForObject("https://api.telegram.org/bot" + token + "/editMessageText", request, EditMessageText.class);
    }

    public void answerInlineQuery(AnswerInlineQuery answerInlineQuery) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AnswerInlineQuery> request = new HttpEntity<>(answerInlineQuery, headers);
            
            String url = "https://api.telegram.org/bot" + token + "/answerInlineQuery";
            String response = restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            System.err.println("Error answering inline query: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteMessage(String chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DeleteMessage> request = new HttpEntity<>(deleteMessage, headers);
        restTemplate.postForObject("https://api.telegram.org/bot" + token + "/deleteMessage", request, DeleteMessage.class);
    }

    public void deleteMessage(DeleteMessage deleteMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DeleteMessage> request = new HttpEntity<>(deleteMessage, headers);
            restTemplate.postForObject("https://api.telegram.org/bot" + token + "/deleteMessage", request, DeleteMessage.class);
        } catch (Exception ignored) {
        }
    }

    public void testBotToken() {
        try {
            System.out.println("🧪 Testing bot token...");
            String url = "https://api.telegram.org/bot" + token + "/getMe";
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("✅ Bot info: " + response);
        } catch (Exception e) {
            System.err.println("❌ Error testing bot token: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
