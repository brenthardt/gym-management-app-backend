package org.example.project1.telegram;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.BotStep;
import org.example.project1.entity.User;
import org.example.project1.repository.UserRepository;
import org.example.project1.service.telegramservice.TelegramServiceImpl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TelegramBot {
    private final UserRepository userRepository;
    private final TelegramServiceImpl telegramService;

    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        User user = userRepository.findByTelegramChatId(chatId).orElseGet(() -> {
            User u = new User();
            u.setTelegramChatId(Long.valueOf(String.valueOf(chatId)));
            u.setStep(BotStep.START);
            return userRepository.save(u);
        });
        if (message.hasContact()) {
            user.setPhone(message.getContact().getPhoneNumber());
            user.setStep(BotStep.START);
            userRepository.save(user);
            SendMessage send = new SendMessage(chatId.toString(), "Telefon raqamingiz qabul qilindi.");
            telegramService.sendMessage(send);
        } else if (message.hasText()) {
            String text = message.getText();
            if (text.equals("/start")) {
                SendMessage send = new SendMessage(chatId.toString(), "Xush kelibsiz. Telefon raqamingizni yuboring.");
                send.setReplyMarkup(phoneButton());
                telegramService.sendMessage(send);
                user.setStep(BotStep.SEND_PHONE);
                userRepository.save(user);
            }
        }
    }

    private ReplyKeyboard phoneButton() {
        KeyboardButton button = new KeyboardButton("📱 Raqamni yuborish");
        button.setRequestContact(true);
        KeyboardRow row = new KeyboardRow();
        row.add(button);
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setKeyboard(List.of(row));
        return markup;
    }
}
