package org.example.project1.telegram;

import lombok.RequiredArgsConstructor;
import org.example.project1.dto.LoginDto;
import org.example.project1.dto.LoginResponse;
import org.example.project1.entity.Role;
import org.example.project1.entity.User;
import org.example.project1.service.userservice.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramWebhookBot {
    private final org.example.project1.service.gymservice.GymService gymService;
    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.username}")
    private String botUsername;
    @Value("${telegram.bot.webhook-path}")
    private String webhookPath;

    private final UserService userService;
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotPath() {
        return webhookPath;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            String data = update.getCallbackQuery().getData();
            SessionState state = sessions.getOrDefault(chatId, new SessionState());
            if (data.startsWith("select_gym_")) {
                String gymId = data.replace("select_gym_", "");
                state.step = org.example.project1.entity.BotStep.SUPERADMIN_MENU;
                sessions.put(chatId, state);
                return buildAdminSelectionMessage(chatId, gymId);
            }
            if (data.equals("add_gym")) {
                state.step = org.example.project1.entity.BotStep.ADD_GYM;
                sessions.put(chatId, state);
                return SendMessage.builder().chatId(chatId).text("Yangi gym nomini kiriting:").build();
            }
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText();
            SessionState state = sessions.getOrDefault(chatId, new SessionState());

            if ("/start".equals(text)) {
                sessions.put(chatId, new SessionState(org.example.project1.entity.BotStep.ASK_USERNAME));
                return SendMessage.builder().chatId(chatId).text("Login uchun telefon raqamingizni kiriting:").build();
            }

            if (state.step == org.example.project1.entity.BotStep.ASK_USERNAME) {
                state.username = text;
                state.step = org.example.project1.entity.BotStep.ASK_PASSWORD;
                sessions.put(chatId, state);
                return SendMessage.builder().chatId(chatId).text("Parolni kiriting:").build();
            }

            if (state.step == org.example.project1.entity.BotStep.ASK_PASSWORD) {
                state.password = text;
                LoginDto loginDto = new LoginDto();
                loginDto.setPhone(state.username);
                loginDto.setPassword(state.password);
                var response = userService.login(loginDto);
                if (response.getStatusCode().is2xxSuccessful()) {
                    LoginResponse loginResponse = (LoginResponse) response.getBody();
                    User user = new User();
                    user.setId(loginResponse.getId());
                    user.setName(loginResponse.getName());
                    user.setPhone(loginResponse.getPhone());
                    if ("SUPERADMIN".equals(loginResponse.getRole())) {
                        Role role = new Role();
                        role.setName("SUPERADMIN");
                        user.setRoles(List.of(role));
                    }
                    state.user = user;
                    boolean isSuperAdmin = user.getRoles() != null && user.getRoles().stream().anyMatch(role -> "SUPERADMIN".equals(role.getName()));
                    if (isSuperAdmin) {
                        state.step = org.example.project1.entity.BotStep.SUPERADMIN_MENU;
                        sessions.put(chatId, state);
                        return buildGymSelectionMessage(chatId);
                    }
                    sessions.remove(chatId);
                    return SendMessage.builder().chatId(chatId).text("Tizimga muvaffaqiyatli kirdingiz.").build();
                } else {
                    sessions.remove(chatId);
                    return SendMessage.builder().chatId(chatId).text("Login yoki parol xato. /start buyrug'ini qayta bosing.").build();
                }
            }

            if (state.step == org.example.project1.entity.BotStep.SUPERADMIN_MENU) {
                if ("1".equals(text)) {
                    state.step = org.example.project1.entity.BotStep.ADD_GYM;
                    sessions.put(chatId, state);
                    return SendMessage.builder().chatId(chatId).text("Yangi gym nomini kiriting:").build();
                }
                if ("2".equals(text)) {
                    return SendMessage.builder().chatId(chatId).text("Gym tanlash funksiyasi tez orada.").build();
                }
                return SendMessage.builder().chatId(chatId).text("Noto'g'ri tanlov. 1 yoki 2 ni kiriting.").build();
            }

            if (state.step == org.example.project1.entity.BotStep.ADD_GYM) {
                state.step = org.example.project1.entity.BotStep.SUPERADMIN_MENU;
                sessions.put(chatId, state);
                return SendMessage.builder().chatId(chatId).text("Gym qo'shildi. Superadmin menyu:").build();
            }
        }
        return null;
    }

    private SendMessage buildGymSelectionMessage(String chatId) {
        return buildGymSelectionMessage(chatId, null);
    }

    private SendMessage buildGymSelectionMessage(String chatId, String selectedGymId) {
        var gymsResponse = gymService.findAll();
        java.util.List<org.example.project1.dto.GymDto> gyms = (java.util.List<org.example.project1.dto.GymDto>) gymsResponse.getBody();
        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup = new org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup();
        java.util.List<java.util.List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        if (gyms != null && !gyms.isEmpty()) {
            for (org.example.project1.dto.GymDto gym : gyms) {
                org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton btn = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
                btn.setText(gym.getName());
                btn.setCallbackData("select_gym_" + gym.getId());
                java.util.List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> row = new java.util.ArrayList<>();
                row.add(btn);
                rows.add(row);
            }
        }
        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton addBtn = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
        addBtn.setText("Yangi gym qo'shish");
        addBtn.setCallbackData("add_gym");
        java.util.List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> addRow = new java.util.ArrayList<>();
        addRow.add(addBtn);
        rows.add(addRow);
        markup.setKeyboard(rows);
        return SendMessage.builder().chatId(chatId).text("Gym tanlang yoki yangi gym qo'shing:").replyMarkup(markup).build();
    }

    private SendMessage buildAdminSelectionMessage(String chatId, String gymId) {
        var gymsResponse = gymService.findAll();
        java.util.List<org.example.project1.dto.GymDto> gyms = (java.util.List<org.example.project1.dto.GymDto>) gymsResponse.getBody();
        org.example.project1.dto.GymDto selectedGym = gyms.stream().filter(g -> g.getId().toString().equals(gymId)).findFirst().orElse(null);
        if (selectedGym == null || selectedGym.getMembers() == null || selectedGym.getMembers().isEmpty()) {
            return SendMessage.builder().chatId(chatId).text("Bu gymda adminlar yo'q. Yangi admin qo'shish uchun kontakt yuboring.").build();
        }
        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup = new org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup();
        java.util.List<java.util.List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        for (org.example.project1.dto.UserDto user : selectedGym.getMembers()) {
            org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton btn = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
            btn.setText(user.getName() + " (" + user.getPhone() + ")");
            btn.setCallbackData("select_admin_" + user.getId());
            java.util.List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> row = new java.util.ArrayList<>();
            row.add(btn);
            rows.add(row);
        }
        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton addBtn = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
        addBtn.setText("Yangi admin qo'shish");
        addBtn.setCallbackData("add_admin_" + gymId);
        java.util.List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> addRow = new java.util.ArrayList<>();
        addRow.add(addBtn);
        rows.add(addRow);
        markup.setKeyboard(rows);
        return SendMessage.builder().chatId(chatId).text("Admin tanlang yoki yangi admin qo'shing:").replyMarkup(markup).build();
    }

    private static class SessionState {
        org.example.project1.entity.BotStep step = org.example.project1.entity.BotStep.NONE;
        String username;
        String password;
        User user;
        SessionState() {}
        SessionState(org.example.project1.entity.BotStep step) { this.step = step; }
    }

    
}
