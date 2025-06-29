package org.example.project1.telegram;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.BotStep;
import org.example.project1.entity.Role;
import org.example.project1.entity.User;
import org.example.project1.entity.Gym;
import org.example.project1.repository.RoleRepository;
import org.example.project1.repository.SubscriptionRepository;
import org.example.project1.repository.UserRepository;
import org.example.project1.repository.GymRepository;
import org.example.project1.repository.SubscriptionTypeRepository;
import org.example.project1.service.telegramservice.TelegramService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.*;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.project1.service.subscriptionservice.SubscriptionService;
import java.time.LocalDate;
import org.example.project1.entity.Subscription;
import org.example.project1.entity.SubscriptionType;

@Component
@RequiredArgsConstructor
public class TelegramBot {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GymRepository gymRepository;
    private final TelegramService telegramService;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTypeRepository subscriptionTypeRepository;

    @Transactional
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasInlineQuery()) {
                String query = update.getInlineQuery().getQuery();
                String queryId = update.getInlineQuery().getId();
                Long adminTgId = update.getInlineQuery().getFrom().getId();
                User admin = userRepository.findByTelegramChatId(adminTgId).orElse(null);
                handleUserInlineQuery(query, queryId, admin);
                return;
            }
            if (update.hasCallbackQuery()) {
                String data = update.getCallbackQuery().getData();
                Long adminTgId = update.getCallbackQuery().getFrom().getId();
                User admin = userRepository.findByTelegramChatId(adminTgId).orElse(null);
                if (data.startsWith("startsub_")) {
                    String userId = data.substring("startsub_".length());
                    User u = userRepository.findById(UUID.fromString(userId)).orElse(null);
                    if (u != null && admin != null) {
                        boolean isMember = false;
                        for (Gym gym : admin.getGyms()) {
                            if (u.getGyms().contains(gym)) {
                                isMember = true;
                                break;
                            }
                        }
                        if (isMember && !u.getSubscriptionTypes().isEmpty()) {
                            SubscriptionType st = u.getSubscriptionTypes().get(0);
                            int duration = st.getDuration();
                            String type = st.getType();
                            if (type != null && type.equalsIgnoreCase("kunora")) {
                                duration = Math.max(12, duration / 2);
                            } else if (type != null && type.equalsIgnoreCase("har kun")) {
                                duration = Math.max(26, duration - 4);
                            }
                            LocalDate start = LocalDate.now();
                            LocalDate end = start.plusDays(duration);
                            Subscription sub = Subscription.builder()
                                    .user(u)
                                    .subscriptionType(st)
                                    .startDate(start)
                                    .endDate(end)
                                    .duration(duration)
                                    .status(true)
                                    .limited(st.getType() != null && st.getType().equalsIgnoreCase("kunora"))
                                    .price(st.getPrice())
                                    .lastDecrementDate(start)
                                    .build();
                            subscriptionRepository.save(sub);
                            StringBuilder info = new StringBuilder();
                            info.append(u.getName()).append(" (").append(u.getPhone()).append(")\n");
                            info.append("Obuna boshlandi!\n");
                            info.append("Boshlanish: ").append(start).append("\n");
                            info.append("Tugash: ").append(end).append("\n");
                            info.append("Narx: ").append(st.getPrice()).append("\n");
                            info.append("Cheklangan: ").append(st.getType()).append("\n");
                            EditMessageText edit = new EditMessageText();
                            edit.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
                            edit.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                            edit.setText(info.toString());
                            telegramService.editMessageText(edit);
                        }
                    }
                    return;
                }
                CallbackQuery callbackQuery = update.getCallbackQuery();
                Long chatId = callbackQuery.getMessage().getChatId();

                List<User> usersWithChatId = userRepository.findAllByTelegramChatId(chatId);
                User user = null;

                if (!usersWithChatId.isEmpty()) {
                    if (usersWithChatId.size() > 1) {
                        System.out.println("⚠️ Found " + usersWithChatId.size() + " duplicate users for chat ID: " + chatId + ". Cleaning up...");
                        user = usersWithChatId.get(0);
                        cleanupDuplicateUsers(chatId);
                        user = userRepository.findByTelegramChatIdWithRoles(chatId).orElse(user);
                        System.out.println("✅ Duplicate users cleaned up for chat ID: " + chatId);
                    } else {
                        user = usersWithChatId.get(0);
                        user = userRepository.findByTelegramChatIdWithRoles(chatId).orElse(user);
                    }
                }

                if (user != null && user.getStep() == BotStep.WAITING_FOR_REPORT_TYPE) {
                    handleReportRequest(user, data, chatId);
                    return;
                }

                if (data.startsWith("edit_tariff_")) {
                    String tariffId = data.substring("edit_tariff_".length());
                    handleEditTariff(user, tariffId, chatId, callbackQuery.getMessage().getMessageId());
                    return;
                }

                if (data.startsWith("delete_tariff_")) {
                    String tariffId = data.substring("delete_tariff_".length());
                    handleDeleteTariff(user, tariffId, chatId, callbackQuery.getMessage().getMessageId());
                    return;
                }

                if (data.startsWith("edit_user_")) {
                    String userId = data.substring("edit_user_".length());
                    handleEditUser(user, userId, chatId, callbackQuery.getMessage().getMessageId());
                    return;
                }

                if (data.startsWith("delete_user_")) {
                    String userId = data.substring("delete_user_".length());
                    handleDeleteUser(user, userId, chatId, callbackQuery.getMessage().getMessageId());
                    return;
                }
            }
            if (!update.hasMessage()) return;
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            createRolesIfNotExist();
            boolean isStart = message.hasText() && message.getText().equals("/start");
            if (isStart) {
                List<User> usersWithChatId = userRepository.findAllByTelegramChatId(chatId);
                if (!usersWithChatId.isEmpty()) {
                    User existingUser = usersWithChatId.get(0);
                    existingUser.setStep(null);
                    existingUser.setTelegramChatId(null);
                    userRepository.save(existingUser);

                    if (usersWithChatId.size() > 1) {
                        userRepository.deleteDuplicateTelegramChatIdUsers();
                    }
                }
                SendMessage send = new SendMessage(chatId.toString(), "Iltimos, telefon raqamingizni yuboring.");
                send.setReplyMarkup(phoneButton());
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex1) {
                    throw ex1;
                }
                return;
            }
            if (message.hasContact()) {
                String phone = normalizePhoneNumber(message.getContact().getPhoneNumber());
                User dbUser = userRepository.findFirstByPhone(phone).orElse(null);
                if (dbUser != null) {
                    dbUser.setTelegramChatId(chatId);
                    dbUser.setStep(BotStep.WAITING_FOR_PASSWORD);
                    userRepository.save(dbUser);
                    SendMessage send = new SendMessage(chatId.toString(), "Iltimos, parolingizni kiriting:");
                    send.setReplyMarkup(new org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove(true));
                    try {
                        telegramService.sendMessage(send);
                    } catch (RuntimeException ex2) {
                        throw ex2;
                    }
                    return;
                } else {
                    User newUser = new User();
                    newUser.setPhone(phone);
                    String tgName = message.getContact().getFirstName() != null ? message.getContact().getFirstName() : "";
                    if (message.getContact().getLastName() != null) {
                        tgName += " " + message.getContact().getLastName();
                    }
                    newUser.setName(tgName.trim().isEmpty() ? "Telegram user" : tgName.trim());
                    newUser.setPassword(passwordEncoder.encode(phone));
                    newUser.setTelegramChatId(chatId);
                    newUser.setStep(BotStep.NONE);
                    Role userRole = roleRepository.findByName("ROLE_USER").orElse(null);
                    if (userRole != null) {
                        newUser.setRoles(new ArrayList<>());
                        newUser.getRoles().add(userRole);
                    }
                    userRepository.save(newUser);
                    StringBuilder userInfo = new StringBuilder();
                    userInfo.append("✅ Siz muvaffaqiyatli ro'yxatdan o'tdingiz!\n\n");
                    userInfo.append("👤 Foydalanuvchi ma'lumotlari:\n");
                    userInfo.append("Ism: ").append(newUser.getName()).append("\n");
                    userInfo.append("📱 Telefon: ").append(newUser.getPhone()).append("\n");
                    userInfo.append("🔑 Role: ROLE_USER\n");
                    userInfo.append("🏋️ Gym: Biriktirilmagan\n");
                    userInfo.append("📊 Ta'rif: Biriktirilmagan\n");
                    SendMessage send = new SendMessage(chatId.toString(), userInfo.toString());
                    send.setReplyMarkup(new org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove(true));
                    try {
                        telegramService.sendMessage(send);
                    } catch (RuntimeException ex3) {
                        throw ex3;
                    }
                    return;
                }
            }

            List<User> usersWithChatId = userRepository.findAllByTelegramChatId(chatId);
            User user = null;

            if (usersWithChatId.isEmpty()) {
                SendMessage send = new SendMessage(chatId.toString(), "Iltimos, telefon raqamingizni yuboring.");
                send.setReplyMarkup(phoneButton());
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex4) {
                    throw ex4;
                }
                return;
            } else if (usersWithChatId.size() > 1) {
                System.out.println("⚠️ Found " + usersWithChatId.size() + " duplicate users for chat ID: " + chatId + ". Cleaning up...");
                user = usersWithChatId.get(0);
                cleanupDuplicateUsers(chatId);
                user = userRepository.findByTelegramChatIdWithRoles(chatId).orElse(user);
                System.out.println("✅ Duplicate users cleaned up for chat ID: " + chatId);
            } else {
                user = usersWithChatId.get(0);
                user = userRepository.findByTelegramChatIdWithRoles(chatId).orElse(user);
            }

            if (user == null) {
                SendMessage send = new SendMessage(chatId.toString(), "Iltimos, telefon raqamingizni yuboring.");
                send.setReplyMarkup(phoneButton());
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex5) {
                    throw ex5;
                }
                return;
            }

            String roleName = user.getRoles() != null && !user.getRoles().isEmpty() ? user.getRoles().get(0).getName() : "";
            if (roleName.equals("ROLE_USER")) {
                SendMessage send = new SendMessage(chatId.toString(), "Sizda admin huquqlari mavjud emas.");
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex6) {
                    throw ex6;
                }
                return;
            }
            if (roleName.equals("ROLE_SUPERADMIN")) {
                long gymCount = gymRepository.count();
                long userCount = userRepository.count();
                List<User> admins = userRepository.findByRoleName("ROLE_ADMIN");
                List<User> superAdmins = userRepository.findByRoleName("ROLE_SUPERADMIN");
                StringBuilder superAdminInfo = new StringBuilder();
                superAdminInfo.append("🦸 SuperAdmin panel\n\n");
                superAdminInfo.append("🏋️ Gymlar soni: ").append(gymCount).append("\n");
                superAdminInfo.append("👥 Foydalanuvchilar soni: ").append(userCount).append("\n");
                superAdminInfo.append("👑 Adminlar soni: ").append(admins.size()).append("\n");
                superAdminInfo.append("🦸 SuperAdminlar soni: ").append(superAdmins.size()).append("\n\n");
                superAdminInfo.append("📱 Admin telefon raqamlari:\n");
                for (User admin : admins) {
                    superAdminInfo.append(admin.getPhone()).append("\n");
                }
                SendMessage send = new SendMessage(chatId.toString(), superAdminInfo.toString());
                send.setReplyMarkup(new org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove(true));
            }
            if (roleName.equals("ROLE_ADMIN")) {
                if (message.hasText()) {
                    String text = message.getText();
                    if (text.contains("Kunlar:")) {
                        int start = text.indexOf("(+998");
                        int end = text.indexOf(")", start);
                        if (start != -1 && end != -1) {
                            String phone = text.substring(start + 1, end);
                            User foundUser = userRepository.findByPhone(phone).orElse(null);
                            // Tariff/dayCount logic removed. No action needed here anymore.
                        }
                    }

                    if (text.contains("#id:")) {
                        String[] parts = text.split("#id:");
                        if (parts.length == 2) {
                            String userIdStr = parts[1].trim();
                            try {
                                User userToUpdate = userRepository.findById(UUID.fromString(userIdStr)).orElse(null);
                                if (userToUpdate == null) {
                                    SendMessage send = new SendMessage(chatId.toString(), "❌ Foydalanuvchi topilmadi.");
                                    telegramService.sendMessage(send);
                                    return;
                                }
                                Subscription activeSub = userToUpdate.getSubscriptions().stream().filter(Subscription::isStatus).findFirst().orElse(null);
                                if (activeSub == null) {
                                    SendMessage send = new SendMessage(chatId.toString(), "❌ Foydalanuvchida aktiv obuna yo'q.");
                                    telegramService.sendMessage(send);
                                    return;
                                }
                                LocalDate today = LocalDate.now();
                                if (activeSub.getLastDecrementDate() != null && activeSub.getLastDecrementDate().isEqual(today)) {
                                    SendMessage send = new SendMessage(chatId.toString(), "❌ Bugun bu foydalanuvchining kuni allaqachon kamaytirilgan.");
                                    telegramService.sendMessage(send);
                                    return;
                                }
                                if (activeSub.getDuration() <= 0) {
                                    SendMessage send = new SendMessage(chatId.toString(), "❌ Foydalanuvchining kunlari tugagan.");
                                    telegramService.sendMessage(send);
                                    return;
                                }
                                activeSub.setDuration(activeSub.getDuration() - 1);
                                activeSub.setLastDecrementDate(today);
                                subscriptionRepository.save(activeSub);
                                StringBuilder info = new StringBuilder();
                                info.append("Ism: ").append(userToUpdate.getName()).append("\n");
                                info.append("Phone: ").append(userToUpdate.getPhone()).append("\n");
                                info.append("Kuni 1 kunga kamaydi. Qolgan kunlar: ").append(activeSub.getDuration());
                                SendMessage send = new SendMessage(chatId.toString(), info.toString());
                                telegramService.sendMessage(send);
                            } catch (Exception e) {
                                SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik: " + e.getMessage());
                                telegramService.sendMessage(send);
                            }
                            return;
                        }
                    }

                    if (text.equals("👤 Add User") || text.equals("➕ Ta'rif qo'shish") ||
                            text.equals("📊 Ta'riflarni ko'rish") || text.equals("📈 Hisobot") || text.equals("👥 Foydalanuvchilarni ko'rish")) {

                        if (user.getStep() != null && user.getStep() != BotStep.NONE) {
                            user.setStep(BotStep.NONE);
                            user.setTempData(null);
                            userRepository.save(user);
                        }

                        switch (text) {
                            case "👤 Add User":
                                startAddUser(user, chatId);
                                return;
                            case "➕ Ta'rif qo'shish":
                                startAddTariff(user, chatId);
                                return;
                            case "📊 Ta'riflarni ko'rish":
                                showTariffs(user, chatId);
                                return;
                            case "📈 Hisobot":
                                showReportOptions(user, chatId);
                                return;
                            case "👥 Foydalanuvchilarni ko'rish":
                                showUsers(user, chatId);
                                return;
                        }
                    }
                    if (text.startsWith("@")) {
                        String query = text.substring(1);
                        List<User> users = new ArrayList<>();
                        if (query.startsWith("+998")) {
                            users = userRepository.findByPhoneStartingWith(query);
                        } else if (!query.isEmpty()) {
                            users = userRepository.findByNameContainingIgnoreCase(query);
                        }
                        List<Gym> adminGyms = user.getGyms();
                        List<User> filtered = new ArrayList<>();
                        for (User u : users) {
                            boolean isAdmin = u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r.getName().contains("ADMIN"));
                            if (isAdmin) continue;
                            for (Gym gym : u.getGyms()) {
                                if (adminGyms.contains(gym)) {
                                    filtered.add(u);
                                    break;
                                }
                            }
                        }
                        for (User u : filtered) {
                            Subscription active = u.getSubscriptions().stream().filter(Subscription::isStatus).findFirst().orElse(null);
                            StringBuilder info = new StringBuilder();
                            info.append(u.getName()).append(" (").append(u.getPhone()).append(")\n");
                            if (active != null) {
                                SubscriptionType st = active.getSubscriptionType();
                                info.append("📊 Obuna: ").append(st.getName()).append("\n");
                                info.append("💰 Narx: ").append(st.getPrice()).append(" so'm\n");
                                info.append("⏰ Muddat: ").append(active.getDuration()).append(" kun\n");
                                info.append("Boshlanish: ").append(active.getStartDate()).append("\n");
                                info.append("Tugash: ").append(active.getEndDate()).append("\n");
                            } else {
                                info.append("   - Obuna: Biriktirilmagan\n");
                            }
                            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                            if (active == null && !u.getSubscriptionTypes().isEmpty()) {
                                InlineKeyboardButton btn = new InlineKeyboardButton();
                                btn.setText("Obunani boshlash");
                                btn.setCallbackData("startsub_" + u.getId());
                                List<InlineKeyboardButton> row = new ArrayList<>();
                                row.add(btn);
                                rows.add(row);
                            }
                            markup.setKeyboard(rows);
                            SendMessage send = new SendMessage(chatId.toString(), info.toString());
                            send.setReplyMarkup(markup);
                            telegramService.sendMessage(send);
                        }
                        return;
                    }
                }

                if (user.getStep() != null && user.getStep() != BotStep.NONE) {
                    if (handleUserSteps(user, message)) {
                        return;
                    }
                }
            }
            // YANGI: incday_ bilan boshlanadigan message uchun duration kamaytirish
            if (message.hasText() && message.getText().startsWith("incday_")) {
                String userIdStr = message.getText().substring("incday_".length());
                try {
                    User userToUpdate = userRepository.findById(UUID.fromString(userIdStr)).orElse(null);
                    if (userToUpdate == null) {
                        SendMessage send = new SendMessage(chatId.toString(), "❌ Foydalanuvchi topilmadi.");
                        telegramService.sendMessage(send);
                        return;
                    }
                    Subscription activeSub = userToUpdate.getSubscriptions().stream().filter(Subscription::isStatus).findFirst().orElse(null);
                    if (activeSub == null) {
                        SendMessage send = new SendMessage(chatId.toString(), "❌ Foydalanuvchida aktiv obuna yo'q.");
                        telegramService.sendMessage(send);
                        return;
                    }
                    LocalDate today = LocalDate.now();
                    if (activeSub.getLastDecrementDate() != null && activeSub.getLastDecrementDate().isEqual(today)) {
                        SendMessage send = new SendMessage(chatId.toString(), "❌ Bugun bu foydalanuvchining kuni allaqachon kamaytirilgan.");
                        telegramService.sendMessage(send);
                        return;
                    }
                    if (activeSub.getDuration() <= 0) {
                        SendMessage send = new SendMessage(chatId.toString(), "❌ Foydalanuvchining kunlari tugagan.");
                        telegramService.sendMessage(send);
                        return;
                    }
                    activeSub.setDuration(activeSub.getDuration() - 1);
                    activeSub.setLastDecrementDate(today);
                    subscriptionRepository.save(activeSub);
                    SendMessage send = new SendMessage(chatId.toString(), "✅ " + userToUpdate.getName() + " gymga keldi. Uning kuni 1 kunga kamaydi. Qolgan kunlar: " + activeSub.getDuration());
                    telegramService.sendMessage(send);
                } catch (Exception e) {
                    SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik: " + e.getMessage());
                    telegramService.sendMessage(send);
                }
                return;
            }
        } catch (Exception e) {
            System.err.println("Error in onUpdateReceived: " + e.getMessage());
        }
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }

        String normalized = phone.trim();

        if (normalized.startsWith("+")) {
            return normalized;
        }

        if (normalized.startsWith("998")) {
            return "+" + normalized;
        }

        if (normalized.startsWith("8")) {
            return "+998" + normalized.substring(1);
        }

        if (normalized.startsWith("0")) {
            return "+998" + normalized.substring(1);
        }

        return normalized;
    }

    private void requestPassword(Long chatId, User user) {
        SendMessage send = new SendMessage(chatId.toString(), "Iltimos, parolni matn ko'rinishida kiriting (kamida 4 ta belgi) va '/' bilan boshlanmasin:");
        telegramService.sendMessage(send);
    }

    private boolean handleUserSteps(User user, Message message) {
        String messageText = message.hasText() ? message.getText() : "";
        Long chatId = message.getChatId();

        switch (user.getStep()) {
            case WAITING_FOR_PASSWORD:
                if (passwordEncoder.matches(messageText, user.getPassword())) {
                    user.setStep(BotStep.NONE);
                    userRepository.save(user);
                    StringBuilder userInfo = new StringBuilder();
                    userInfo.append("✅ Siz muvaffaqiyatli ro'yxatdan o'tdingiz!\n\n");
                    userInfo.append("👤 Foydalanuvchi ma'lumotlari:\n");
                    userInfo.append("📱 Telefon: ").append(user.getPhone()).append("\n");
                    String roleName = user.getRoles() != null && !user.getRoles().isEmpty() ? user.getRoles().get(0).getName() : "";
                    userInfo.append("🔑 Role: ").append(roleName).append("\n");
                    if (roleName.equals("ROLE_USER")) {
                        if (!user.getGyms().isEmpty()) {
                            Gym gym = user.getGyms().get(0);
                            userInfo.append("🏋️ Gym: ").append(gym.getName()).append(" (" + gym.getLocation() + ")\n");
                        } else {
                            userInfo.append("🏋️ Gym: Biriktirilmagan\n");
                        }
                        Subscription active = user.getSubscriptions().stream().filter(Subscription::isStatus).findFirst().orElse(null);
                        if (active != null) {
                            SubscriptionType st = active.getSubscriptionType();
                            userInfo.append("📊 Obuna: ").append(st.getName()).append("\n");
                            userInfo.append("💰 Narx: ").append(st.getPrice()).append(" so'm\n");
                            userInfo.append("⏰ Muddat: ").append(active.getDuration()).append(" kun\n");
                            userInfo.append("Boshlanish: ").append(active.getStartDate()).append("\n");
                            userInfo.append("Tugash: ").append(active.getEndDate()).append("\n");
                        } else {
                            userInfo.append("   - Obuna: Biriktirilmagan\n");
                        }
                    } else if (!user.getSubscriptionTypes().isEmpty()) {
                        SubscriptionType userTariff = user.getSubscriptionTypes().get(0);
                        userInfo.append("📊 Ta'rif: ").append(userTariff.getName()).append("\n");
                        userInfo.append("💰 Narx: ").append(userTariff.getPrice()).append(" so'm\n");
                        userInfo.append("⏰ Muddat: ").append(userTariff.getDuration()).append(" kun\n");
                        userInfo.append("📅 Kelgan kunlar: ");
                    }
                    SendMessage send;
                    if (roleName.equals("ROLE_ADMIN")) {
                        send = new SendMessage(chatId.toString(), userInfo.toString());
                        send.setReplyMarkup(adminKeyboard());
                    } else if (roleName.equals("ROLE_SUPERADMIN")) {
                        long gymCount = gymRepository.count();
                        long userCount = userRepository.count();
                        List<User> admins = userRepository.findByRoleName("ROLE_ADMIN");
                        List<User> superAdmins = userRepository.findByRoleName("ROLE_SUPERADMIN");
                        StringBuilder superAdminInfo = new StringBuilder();
                        superAdminInfo.append("🦸 SuperAdmin panel\n\n");
                        superAdminInfo.append("🏋️ Gymlar soni: ").append(gymCount).append("\n");
                        superAdminInfo.append("👥 Foydalanuvchilar soni: ").append(userCount).append("\n");
                        superAdminInfo.append("👑 Adminlar soni: ").append(admins.size()).append("\n");
                        superAdminInfo.append("🦸 SuperAdminlar soni: ").append(superAdmins.size()).append("\n\n");
                        superAdminInfo.append("📱 Admin telefon raqamlari:\n");
                        for (User admin : admins) {
                            superAdminInfo.append(admin.getPhone()).append("\n");
                        }
                        send = new SendMessage(chatId.toString(), superAdminInfo.toString());
                        send.setReplyMarkup(new org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove(true));
                    } else {
                        send = new SendMessage(chatId.toString(), userInfo.toString());
                        send.setReplyMarkup(new org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove(true));
                    }
                    telegramService.sendMessage(send);
                } else {
                    SendMessage send = new SendMessage(chatId.toString(), "❌ Noto'g'ri parol. Qaytadan urinib ko'ring:");
                    send.setReplyMarkup(new org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove(true));
                    telegramService.sendMessage(send);
                }
                return true;

            case ADD_USER_NAME:
                if (!messageText.isBlank() && messageText.trim().length() >= 2) {
                    handleAddUserName(user, messageText.trim(), chatId);
                } else {
                    SendMessage send = new SendMessage(chatId.toString(), "❌ Iltimos, foydalanuvchi ismini kiriting (kamida 2 ta harf):");
                    try {
                        telegramService.sendMessage(send);
                    } catch (RuntimeException ex10) {
                        throw ex10;
                    }
                }
                return true;

            case ADD_USER_PHONE:
                String normalizedPhone = normalizePhoneNumber(messageText);
                if (normalizedPhone != null && normalizedPhone.matches("\\+998[0-9]{9}")) {
                    handleAddUserPhone(user, messageText, chatId);
                } else {
                    SendMessage send = new SendMessage(chatId.toString(), "❌ Iltimos, to'g'ri telefon raqam kiriting (+998xxxxxxxxx, 998xxxxxxxxx, 8xxxxxxxxx yoki 0xxxxxxxxx ko'rinishida):");
                    try {
                        telegramService.sendMessage(send);
                    } catch (RuntimeException ex11) {
                        throw ex11;
                    }
                }
                return true;

            case ADD_USER_SELECT_SUBSCRIPTION_TYPE:
                handleAddUserTariff(user, messageText, chatId);
                return true;

            case ADD_USER_DAY_COUNT:
                handleAddUserDayCount(user, messageText, chatId);
                return true;

            case ADD_SUBSCRIPTION_TYPE_NAME:
                handleAddTariffName(user, messageText, chatId);
                return true;

            case ADD_SUBSCRIPTION_TYPE_PRICE:
                handleAddTariffPrice(user, messageText, chatId);
                return true;

            case ADD_SUBSCRIPTION_TYPE_DURATION:
                handleAddTariffDuration(user, messageText, chatId);
                return true;

            case ADD_SUBSCRIPTION_TYPE_DESCRIPTION:
                handleAddTariffDescription(user, messageText, chatId);
                return true;

            case EDIT_SUBSCRIPTION_TYPE_NAME:
                handleEditTariffName(user, messageText, chatId);
                return true;

            case EDIT_SUBSCRIPTION_TYPE_PRICE:
                handleEditTariffPrice(user, messageText, chatId);
                return true;

            case EDIT_SUBSCRIPTION_TYPE_DURATION:
                handleEditTariffDuration(user, messageText, chatId);
                return true;

            case EDIT_SUBSCRIPTION_TYPE_DESCRIPTION:
                handleEditTariffDescription(user, messageText, chatId);
                return true;

            case EDIT_USER_NAME:
                handleEditUserName(user, messageText, chatId);
                return true;

            case EDIT_USER_PHONE:
                handleEditUserPhone(user, messageText, chatId);
                return true;

            case EDIT_USER_SELECT_SUBSCRIPTION_TYPE:
                handleEditUserTariff(user, messageText, chatId);
                return true;

            case EDIT_USER_DAY_COUNT:
                handleEditUserDayCount(user, messageText, chatId);
                return true;

            case WAITING_FOR_REPORT_TYPE:
                handleReportRequest(user, messageText, chatId);
                return true;

            default:
                return false;
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

    private ReplyKeyboard adminKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("👤 Add User"));
        row1.add(new KeyboardButton("➕ Ta'rif qo'shish"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📊 Ta'riflarni ko'rish"));
        row2.add(new KeyboardButton("📈 Hisobot"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("👥 Foydalanuvchilarni ko'rish"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }

    private void showTariffs(User admin, Long chatId) {
        List<Gym> adminGyms = admin.getGyms();
        if (adminGyms.isEmpty()) {
            try {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "Sizga biriktirilgan gym mavjud emas."));
            } catch (RuntimeException ex12) {
                throw ex12;
            }
            return;
        }
        Gym adminGym = adminGyms.get(0);
        List<SubscriptionType> tariffs = subscriptionTypeRepository.findByGym(adminGym);

        SendMessage infoMessage = new SendMessage(chatId.toString(), "📊 Mavjud ta'riflar (" + adminGym.getName() + "):");
        try {
            telegramService.sendMessage(infoMessage);
        } catch (RuntimeException ex13) {
            throw ex13;
        }

        if (tariffs.isEmpty()) {
            SendMessage send = new SendMessage(chatId.toString(), "Hozircha ta'riflar mavjud emas.");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex14) {
                throw ex14;
            }
            return;
        }

        for (SubscriptionType tariff : tariffs) {
            StringBuilder sb = new StringBuilder();
            sb.append("🏷️ ").append(tariff.getName()).append("\n");
            sb.append("💰 Narx: ").append(tariff.getPrice()).append(" so'm\n");
            sb.append("⏰ Muddat: ").append(tariff.getDuration()).append(" kun\n");
            sb.append("👥 Foydalanuvchilar soni: ").append(tariff.getUsers() != null ? tariff.getUsers().size() : 0).append("\n");
            if (tariff.getDescription() != null && !tariff.getDescription().isBlank()) {
                sb.append("📝 Tavsif: ").append(tariff.getDescription()).append("\n");
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton editBtn = new InlineKeyboardButton();
            editBtn.setText("✏️ Edit");
            editBtn.setCallbackData("edit_tariff_" + tariff.getId());
            row.add(editBtn);

            InlineKeyboardButton deleteBtn = new InlineKeyboardButton();
            deleteBtn.setText("🗑️ Delete");
            deleteBtn.setCallbackData("delete_tariff_" + tariff.getId());
            row.add(deleteBtn);

            rows.add(row);
            markup.setKeyboard(rows);

            SendMessage send = new SendMessage(chatId.toString(), sb.toString());
            send.setReplyMarkup(markup);
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex15) {
                throw ex15;
            }
        }
    }

    private void showUsers(User admin, Long chatId) {
        List<Gym> adminGyms = userRepository.findGymsByMemberId(admin.getId());
        if (adminGyms.isEmpty()) {
            telegramService.sendMessage(new SendMessage(chatId.toString(), "Sizga biriktirilgan gym yo'q."));
            return;
        }
        telegramService.sendMessage(new SendMessage(chatId.toString(), "👥 Foydalanuvchilar ro'yxati:"));
        for (Gym gym : adminGyms) {
            List<User> gymUsers = new ArrayList<>();
            for (User user : gym.getMembers()) {
                if (user.getRoles() == null || user.getRoles().isEmpty()) continue;
                String role = user.getRoles().get(0).getName();
                if (!role.equals("ROLE_ADMIN") && !role.equals("ROLE_SUPERADMIN")) {
                    gymUsers.add(user);
                }
            }
            if (gymUsers.isEmpty()) {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "🏋️ " + gym.getName() + ": Bu gymda foydalanuvchilar yo'q."));
                continue;
            }
            for (User user : gymUsers) {
                StringBuilder sb = new StringBuilder();
                sb.append("👤 ").append(user.getName()).append(" | ").append(user.getPhone()).append("\n");
                Subscription active = user.getSubscriptions().stream().filter(Subscription::isStatus).findFirst().orElse(null);
                if (active != null) {
                    SubscriptionType st = active.getSubscriptionType();
                    sb.append("   - Obuna: ").append(st.getName()).append("\n");
                    sb.append("   - Narx: ").append(st.getPrice()).append(" so'm\n");
                    sb.append("   - Muddat: ").append(active.getDuration()).append(" kun\n");
                    sb.append("   - Boshlanish: ").append(active.getStartDate()).append("\n");
                    sb.append("   - Tugash: ").append(active.getEndDate()).append("\n");
                } else {
                    sb.append("   - Obuna: Biriktirilmagan\n");
                }
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                if (active == null && !user.getSubscriptionTypes().isEmpty()) {
                    InlineKeyboardButton btn = new InlineKeyboardButton();
                    btn.setText("Obuna bo'lish");
                    btn.setCallbackData("startsub_" + user.getId());
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    row.add(btn);
                    rows.add(row);
                }
                markup.setKeyboard(rows);
                SendMessage send = new SendMessage(chatId.toString(), sb.toString());
                send.setReplyMarkup(markup);
                telegramService.sendMessage(send);
            }
        }
    }

    private void startAddUser(User admin, Long chatId) {
        admin.setStep(BotStep.ADD_USER_NAME);
        userRepository.save(admin);

        SendMessage send = new SendMessage(chatId.toString(),
                "👤 Yangi foydalanuvchi qo'shish\n\nFoydalanuvchining ismini kiriting:");
        try {
            telegramService.sendMessage(send);
        } catch (RuntimeException ex101) {
            throw ex101;
        }
    }

    private void handleAddUserName(User admin, String name, Long chatId) {
        admin.setStep(BotStep.ADD_USER_PHONE);
        admin.setTempData(name);
        userRepository.save(admin);
        SendMessage send = new SendMessage(chatId.toString(),
                "✅ Ism: " + name + "\n\nEndi telefon raqamini matn ko'rinishida kiriting:");
        try {
            telegramService.sendMessage(send);
        } catch (RuntimeException ex102) {
            throw ex102;
        }
    }

    private void handleAddUserPhone(User admin, String phone, Long chatId) {
        String normalizedPhone = normalizePhoneNumber(phone);
        if (userRepository.findFirstByPhone(normalizedPhone).isPresent()) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Bu telefon raqami allaqachon mavjud. Iltimos, boshqa raqam kiriting.");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex103) {
                throw ex103;
            }
            return;
        }
        String userName = admin.getTempData();
        User newUser = new User();
        newUser.setPhone(normalizedPhone);
        newUser.setName(userName);
        newUser.setPassword(passwordEncoder.encode(normalizedPhone));
        newUser.setStep(BotStep.NONE);
        Role userRole = roleRepository.findByName("ROLE_USER").orElse(null);
        if (userRole != null) {
            newUser.setRoles(new ArrayList<>());
            newUser.getRoles().add(userRole);
        }
        userRepository.save(newUser);
        admin.setTempData(newUser.getId().toString());
        admin.setStep(BotStep.ADD_USER_SELECT_SUBSCRIPTION_TYPE);
        userRepository.save(admin);

        showTariffsForSelection(admin, chatId, "📊 Ta'rifni tanlang:");
    }

    private void showTariffsForSelection(User admin, Long chatId, String messageText) {
        List<Gym> adminGyms = admin.getGyms();
        if (adminGyms.isEmpty()){
            try {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "Sizga gym biriktirilmagan."));
            } catch (RuntimeException ex104) {
                throw ex104;
            }
            return;
        }
        List<SubscriptionType> tariffs = subscriptionTypeRepository.findByGym(adminGyms.get(0));
        StringBuilder sb = new StringBuilder();
        sb.append(messageText).append("\n\n");

        if (tariffs.isEmpty()) {
            sb.append("Ta'riflar mavjud emas.");
        } else {
            for (int i = 0; i < tariffs.size(); i++) {
                SubscriptionType tariff = tariffs.get(i);
                sb.append((i + 1)).append(". ").append(tariff.getName()).append(" - ").append(tariff.getPrice()).append(" so'm\n");
            }
            sb.append("\nTa'rif raqamini kiriting:");
        }

        SendMessage send = new SendMessage(chatId.toString(), sb.toString());
        try {
            telegramService.sendMessage(send);
        } catch (RuntimeException ex105) {
            throw ex105;
        }
    }

    private void showReportOptions(User admin, Long chatId) {
        admin.setStep(BotStep.WAITING_FOR_REPORT_TYPE);
        userRepository.save(admin);
        String text = "📈 Hisobot turlari:\n\n" +
                "1. Barcha foydalanuvchilar\n" +
                "2. Admin foydalanuvchilar\n" +
                "3. Ta'riflar bo'yicha hisobot\n" +
                "4. Gym'lar bo'yicha hisobot\n" +
                "\nKerakli hisobot turini tanlang:";
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(String.valueOf(i));
            btn.setCallbackData(String.valueOf(i));
            row1.add(btn);
        }
        rows.add(row1);
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton btn4 = new InlineKeyboardButton();
        btn4.setText("4");
        btn4.setCallbackData("4");
        row2.add(btn4);
        rows.add(row2);
        markup.setKeyboard(rows);
        SendMessage send = new SendMessage(chatId.toString(), text);
        send.setReplyMarkup(markup);
        try {
            telegramService.sendMessage(send);
        } catch (RuntimeException ex106) {
            throw ex106;
        }
    }

    @Transactional
    protected void handleReportRequest(User admin, String messageText, Long chatId) {
        try {
            admin.setStep(BotStep.NONE);
            userRepository.save(admin);

            String roleName = admin.getRoles() != null && !admin.getRoles().isEmpty() ? admin.getRoles().get(0).getName() : "";
            if (roleName.equals("ROLE_SUPERADMIN")) {
                SendMessage send = new SendMessage(chatId.toString(), "🦸 SuperAdmin muvaffaqiyatli ro'yxatdan o'tdi.");
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex107) {
                    throw ex107;
                }
                return;
            }

            StringBuilder sb = new StringBuilder();
            switch (messageText) {
                case "1":
                    List<Gym> adminGyms = userRepository.findGymsByMemberId(admin.getId());
                    if (adminGyms.isEmpty()) {
                        sb.append("Sizga biriktirilgan gym yo'q.");
                    } else {
                        for (Gym gym : adminGyms) {
                            List<User> gymUsers = new ArrayList<>();
                            for (User user : gym.getMembers()) {
                                if (user.getRoles() == null || user.getRoles().isEmpty()) continue;
                                String role = user.getRoles().get(0).getName();
                                if (!role.equals("ROLE_ADMIN") && !role.equals("ROLE_SUPERADMIN")) {
                                    gymUsers.add(user);
                                }
                            }
                            sb.append("🏋️ ").append(gym.getName()).append(" (Jami: ").append(gymUsers.size()).append(")\n");
                            if (gymUsers.isEmpty()) {
                                sb.append("Bu gymda foydalanuvchilar yo'q.\n");
                            } else {
                                for (User user : gymUsers) {
                                    sb.append(user.getName()).append(" | ").append(user.getPhone()).append("\n");
                                }
                            }
                            sb.append("─────────────────\n");
                        }
                    }
                    break;
                case "2":
                    sb.append("👑 Admin foydalanuvchilar:\n\n");
                    List<User> admins = userRepository.findByRoleName("ROLE_ADMIN");
                    sb.append("Admin'lar: ").append(admins.size()).append("\n\n");
                    admins.forEach(admin1 -> sb.append("👤 ").append(admin1.getPhone()).append(" (ADMIN)\n"));
                    break;
                case "3":
                    sb.append("📊 Ta'riflar hisoboti:\n\n");
                    List<SubscriptionType> tariffs = subscriptionTypeRepository.findAll();
                    for (SubscriptionType tariff : tariffs) {
                        sb.append("🏷️ ").append(tariff.getName()).append("\n");
                        sb.append("💰 ").append(tariff.getPrice()).append(" so'm\n");
                        sb.append("⏰ Muddat: ").append(tariff.getDuration()).append(" kun\n");
                        int userCount = (tariff.getUsers() == null) ? 0 : tariff.getUsers().size();
                        sb.append("👥 Foydalanuvchilar: ").append(userCount).append("\n");
                        sb.append("─────────────────\n");
                    }
                    break;
                case "4":
                    sb.append("🏋️ Gym'lar hisoboti:\n\n");
                    List<Gym> gyms = gymRepository.findAllWithSubscriptionTypes();
                    sb.append("Jami gym'lar: ").append(gyms.size()).append("\n\n");
                    for (int i = 0; i < gyms.size(); i++) {
                        Gym gym = gyms.get(i);
                        sb.append((i + 1)).append(". 🏋️ ").append(gym.getName()).append("\n");
                        sb.append("   📍 Manzil: ").append(gym.getLocation()).append("\n");
                        if (gym.getSubscriptionTypes() != null && !gym.getSubscriptionTypes().isEmpty()) {
                            sb.append("   📊 Ta'riflar soni: ").append(gym.getSubscriptionTypes().size()).append("\n");
                        }
                        sb.append("   👥 A'zolar soni: ").append(gym.getMembers().size()).append("\n");
                        sb.append("─────────────────\n");
                    }
                    break;
                default:
                    sb.append("❌ Noto'g'ri tanlov. Qaytadan urinib ko'ring.");
                    showReportOptions(admin, chatId);
                    return;
            }
            SendMessage send = new SendMessage(chatId.toString(), sb.toString());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex108) {
                throw ex108;
            }
        } catch (NumberFormatException e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Noto'g'ri kunlar. Iltimos, raqam kiriting:");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex109) {
                throw ex109;
            }
            throw e;
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik yuz berdi: " + e.getMessage());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex110) {
                throw ex110;
            }
            throw e;
        }
    }

    private void createRolesIfNotExist() {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            roleRepository.save(adminRole);
        }

        if (roleRepository.findByName("ROLE_SUPERADMIN").isEmpty()) {
            Role superAdminRole = new Role();
            superAdminRole.setName("ROLE_SUPERADMIN");
            roleRepository.save(superAdminRole);
        }

        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);
        }
    }

    private void startAddTariff(User admin, Long chatId) {
        admin.setStep(BotStep.ADD_SUBSCRIPTION_TYPE_NAME);
        userRepository.save(admin);

        SendMessage send = new SendMessage(chatId.toString(),
                "➕ Yangi ta'rif qo'shish\n\nTa'rif nomini kiriting:");
        try {
            telegramService.sendMessage(send);
        } catch (RuntimeException ex111) {
            throw ex111;
        }
    }

    private void handleAddTariffName(User admin, String name, Long chatId) {
        admin.setStep(BotStep.ADD_SUBSCRIPTION_TYPE_PRICE);
        admin.setTempData(name);
        userRepository.save(admin);

        SendMessage send = new SendMessage(chatId.toString(),
                "✅ Ta'rif nomi: " + name + "\n\nTa'rif narxini kiriting (so'm):");
        try {
            telegramService.sendMessage(send);
        } catch (RuntimeException ex112) {
            throw ex112;
        }
    }

    private void handleAddTariffPrice(User admin, String priceText, Long chatId) {
        try {
            double price = Double.parseDouble(priceText);
            admin.setStep(BotStep.ADD_SUBSCRIPTION_TYPE_DURATION);
            admin.setTempData(admin.getTempData() + "|" + price);
            userRepository.save(admin);
            SendMessage send = new SendMessage(chatId.toString(),
                    "✅ Narx: " + price + " so'm\n\nTa'rif muddatini kiriting (kun):");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex113) {
                throw ex113;
            }
        } catch (NumberFormatException e) {
            SendMessage send = new SendMessage(chatId.toString(),
                    "❌ Noto'g'ri narx. Iltimos, raqam kiriting:");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex114) {
                throw ex114;
            }
            throw e;
        }
    }

    private void handleAddTariffDuration(User admin, String durationText, Long chatId) {
        try {
            int duration = Integer.parseInt(durationText);
            admin.setStep(BotStep.ADD_SUBSCRIPTION_TYPE_DESCRIPTION);
            admin.setTempData(admin.getTempData() + "|" + duration);
            userRepository.save(admin);

            SendMessage send = new SendMessage(chatId.toString(),
                    "✅ Muddat: " + duration + " kun\n\nTa'rif tavsifini kiriting (yoki /skip yozing):");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex115) {
                throw ex115;
            }
        } catch (NumberFormatException e) {
            SendMessage send = new SendMessage(chatId.toString(),
                    "❌ Noto'g'ri muddat. Iltimos, raqam kiriting:");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex116) {
                throw ex116;
            }
            throw e;
        }
    }

    private void handleAddTariffDescription(User admin, String description, Long chatId) {
        List<Gym> adminGyms = admin.getGyms();
        if (adminGyms.isEmpty()) {
            try {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "❌ Xatolik: Sizga gym biriktirilmagan."));
            } catch (RuntimeException ex117) {
                throw ex117;
            }
            return;
        }
        try {
            String[] tempData = admin.getTempData().split("\\|");
            String name = tempData[0];
            double price = Double.parseDouble(tempData[1]);
            int duration = Integer.parseInt(tempData[2]);
            SubscriptionType newTariff = new SubscriptionType();
            newTariff.setName(name);
            newTariff.setPrice(price);
            newTariff.setDuration(duration);
            newTariff.setGym(adminGyms.get(0));
            if (!description.equals("/skip") && !description.isBlank()) {
                newTariff.setDescription(description);
            }
            subscriptionTypeRepository.save(newTariff);
            admin.setTempData(null);
            admin.setStep(BotStep.NONE);
            userRepository.save(admin);
            StringBuilder sb = new StringBuilder();
            sb.append("✅ Ta'rif muvaffaqiyatli qo'shildi!\n\n");
            sb.append("🏷️ Nomi: ").append(name).append("\n");
            sb.append("💰 Narx: ").append(price).append(" so'm\n");
            sb.append("⏰ Muddat: ").append(duration).append(" kun\n");
            if (newTariff.getDescription() != null) {
                sb.append("📝 Tavsif: ").append(newTariff.getDescription()).append("\n");
            }
            SendMessage send = new SendMessage(chatId.toString(), sb.toString());
            send.setReplyMarkup(adminKeyboard());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex118) {
                throw ex118;
            }
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik yuz berdi: " + e.getMessage());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex119) {
                throw ex119;
            }
            throw e;
        }
    }

    private void handleAddUserTariff(User admin, String messageText, Long chatId) {
        List<Gym> adminGyms = admin.getGyms();
        if (adminGyms.isEmpty()){
            try {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "Sizga gym biriktirilmagan."));
            } catch (RuntimeException ex120) {
                throw ex120;
            }
            return;
        }
        List<SubscriptionType> tariffs = subscriptionTypeRepository.findByGym(adminGyms.get(0));
        try {
            int tariffIndex = Integer.parseInt(messageText) - 1;
            if (tariffIndex < 0 || tariffIndex >= tariffs.size()) {
                SendMessage send = new SendMessage(chatId.toString(), "Noto'g'ri ta'rif tanlandi. Qaytadan urinib ko'ring.");
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex121) {
                    throw ex121;
                }
                return;
            }
            SubscriptionType selectedTariff = tariffs.get(tariffIndex);
            String userIdStr = admin.getTempData();
            if (userIdStr == null) {
                SendMessage send = new SendMessage(chatId.toString(), "Foydalanuvchi topilmadi. Qaytadan urinib ko'ring.");
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex122) {
                    throw ex122;
                }
                return;
            }
            User userToAdd = userRepository.findById(UUID.fromString(userIdStr)).orElse(null);
            if (userToAdd == null) {
                SendMessage send = new SendMessage(chatId.toString(), "Foydalanuvchi topilmadi. Qaytadan urinib ko'ring.");
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex123) {
                    throw ex123;
                }
                return;
            }
            if (userToAdd.getSubscriptionTypes() == null) userToAdd.setSubscriptionTypes(new ArrayList<>());
            if (userToAdd.getGyms() == null) userToAdd.setGyms(new ArrayList<>());
            userToAdd.getSubscriptionTypes().add(selectedTariff);
            List<Gym> adminGymsToAdd = admin.getGyms();
            if (adminGymsToAdd != null && !adminGymsToAdd.isEmpty()) {
                userToAdd.getGyms().add(adminGymsToAdd.get(0));
            }
            userToAdd.setStep(BotStep.NONE);
            userRepository.save(userToAdd);
            try {
                subscriptionService.subscribeUserToSubscriptionType(userToAdd, selectedTariff, selectedTariff.getPrice().intValue(), selectedTariff.getDuration());
            } catch (Exception e) {
                System.err.println("❌ Error creating subscription: " + e.getMessage());
            }

            admin.setTempData(selectedTariff.getId().toString());
            admin.setStep(BotStep.ADD_USER_DAY_COUNT);
            userRepository.save(admin);

            SendMessage send = new SendMessage(chatId.toString(), "✅ Ta'rif tanlandi: " + selectedTariff.getName() + "\n\nFoydalanuvchining kelgan kunlar sonini kiriting:");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex124) {
                throw ex124;
            }
        } catch (IllegalArgumentException e) {
            SendMessage send = new SendMessage(chatId.toString(), "Noto'g'ri ta'rif tanlandi yoki foydalanuvchi topilmadi. Qaytadan urinib ko'ring.");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex125) {
                throw ex125;
            }
        }
    }

    private void handleAddUserDayCount(User admin, String dayCountText, Long chatId) {
        try {
            int dayCount = Integer.parseInt(dayCountText);
            String tariffIdStr = admin.getTempData();
            if (tariffIdStr == null) {
                SendMessage send = new SendMessage(chatId.toString(), "Ta'rif topilmadi. Qaytadan urinib ko'ring.");
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex126) {
                    throw ex126;
                }
                return;
            }

            SubscriptionType tariff = subscriptionTypeRepository.findById(UUID.fromString(tariffIdStr)).orElse(null);
            if (tariff == null) {
                SendMessage send = new SendMessage(chatId.toString(), "Ta'rif topilmadi. Qaytadan urinib ko'ring.");
                try {
                    telegramService.sendMessage(send);
                } catch (RuntimeException ex127) {
                    throw ex127;
                }
                return;
            }

            User lastUser = tariff.getUsers().get(tariff.getUsers().size() - 1);

            userRepository.save(lastUser);

            StringBuilder successMessage = new StringBuilder();
            successMessage.append("✅ Foydalanuvchi muvaffaqiyatli qo'shildi!\n\n");
            successMessage.append("👤 Ism: ").append(lastUser.getName()).append("\n");
            successMessage.append("📱 Telefon: ").append(lastUser.getPhone()).append("\n");
            successMessage.append("📊 Ta'rif: ").append(tariff.getName()).append("\n");
            successMessage.append("💰 Narx: ").append(tariff.getPrice()).append(" so'm\n");
            successMessage.append("⏰ Muddat: ").append(tariff.getDuration()).append(" kun\n");
            successMessage.append("📅 Kelgan kunlar: ").append(dayCount).append(" kun\n");

            List<Gym> adminGyms = admin.getGyms();
            if (adminGyms != null && !adminGyms.isEmpty()) {
                successMessage.append("🏋️ Gym: ").append(adminGyms.get(0).getName()).append("\n");
            }

            admin.setTempData(null);
            admin.setStep(BotStep.NONE);
            userRepository.save(admin);

            SendMessage send = new SendMessage(chatId.toString(), successMessage.toString());
            send.setReplyMarkup(adminKeyboard());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex128) {
                throw ex128;
            }
        } catch (NumberFormatException e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Noto'g'ri kunlar. Iltimos, raqam kiriting:");
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex129) {
                throw ex129;
            }
            throw e;
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik yuz berdi: " + e.getMessage());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex130) {
                throw ex130;
            }
            throw e;
        }
    }

    @Transactional
    public void cleanupDuplicateUsers(Long chatId) {
        try {
            userRepository.deleteSubscriptionsForDuplicateUsers(chatId);
            userRepository.deleteAllSubscriptionsForDuplicateUsers();
            userRepository.deleteDuplicateTelegramChatIdUsers();
        } catch (Exception e) {
            System.err.println("❌ Error cleaning up duplicate users for chat ID " + chatId + ": " + e.getMessage());
            throw e;
        }
    }

    private void handleEditTariff(User admin, String tariffId, Long chatId, Integer messageId) {
        SubscriptionType tariff = subscriptionTypeRepository.findById(UUID.fromString(tariffId)).orElse(null);
        if (tariff == null) {
            try {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "❌ Ta'rif topilmadi."));
            } catch (RuntimeException ex131) {
                throw ex131;
            }
            return;
        }

        admin.setStep(BotStep.EDIT_SUBSCRIPTION_TYPE_NAME);
        admin.setTempData(tariffId + "|" + messageId);
        userRepository.save(admin);

        String text = "✏️ Ta'rifni tahrirlash\n\n" +
                "Hozirgi nomi: " + tariff.getName() + "\n" +
                "Yangi nomini kiriting:";

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(text);
        try {
            telegramService.editMessageText(editMessage);
        } catch (RuntimeException ex132) {
            throw ex132;
        }
    }

    @Transactional
    public void handleDeleteTariff(User admin, String tariffId, Long chatId, Integer messageId) {
        try {
            SubscriptionType tariff = subscriptionTypeRepository.findById(UUID.fromString(tariffId)).orElse(null);
            if (tariff == null) {
                try {
                    telegramService.sendMessage(new SendMessage(chatId.toString(), "❌ Ta'rif topilmadi."));
                } catch (RuntimeException ex133) {
                    throw ex133;
                }
                return;
            }

            subscriptionRepository.deleteAllBySubscriptionType(tariff);

            List<User> usersWithTariff = tariff.getUsers();
            if (usersWithTariff != null) {
                for (User user : new ArrayList<>(usersWithTariff)) {
                    user.getSubscriptionTypes().remove(tariff);
                }
            }

            subscriptionTypeRepository.delete(tariff);
            try {
                telegramService.deleteMessage(chatId.toString(), messageId);
            } catch (RuntimeException ex134) {
                throw ex134;
            }
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Ta'rifni o'chirishda xatolik yuz berdi: " + e.getMessage());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex135) {
                throw ex135;
            }
            throw e;
        }
    }

    private void handleEditTariffName(User admin, String name, Long chatId) {
        String[] tempData = admin.getTempData().split("\\|");
        String messageId = tempData[1];

        admin.setStep(BotStep.EDIT_SUBSCRIPTION_TYPE_PRICE);
        admin.setTempData(admin.getTempData() + "|" + name);
        userRepository.save(admin);

        String text = "✅ Yangi nomi: " + name + "\n\nYangi narxini kiriting (so'm):";
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(Integer.parseInt(messageId));
        editMessage.setText(text);
        try {
            telegramService.editMessageText(editMessage);
        } catch (RuntimeException ex136) {
            throw ex136;
        }
    }

    private void handleEditTariffPrice(User admin, String priceText, Long chatId) {
        String[] tempData = admin.getTempData().split("\\|");
        String messageId = tempData[1];
        try {
            double price = Double.parseDouble(priceText);
            admin.setStep(BotStep.EDIT_SUBSCRIPTION_TYPE_DURATION);
            admin.setTempData(admin.getTempData() + "|" + price);
            userRepository.save(admin);

            String text = "✅ Yangi narxi: " + price + " so'm\n\nYangi muddatini kiriting (kun):";
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText(text);
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex137) {
                throw ex137;
            }
        } catch (NumberFormatException e) {
            String text = "❌ Noto'g'ri narx. Iltimos, raqam kiriting:";
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText(text);
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex138) {
                throw ex138;
            }
            throw e;
        }
    }

    private void handleEditTariffDuration(User admin, String durationText, Long chatId) {
        String[] tempData = admin.getTempData().split("\\|");
        String messageId = tempData[1];
        try {
            int duration = Integer.parseInt(durationText);
            admin.setStep(BotStep.EDIT_SUBSCRIPTION_TYPE_DESCRIPTION);
            admin.setTempData(admin.getTempData() + "|" + duration);
            userRepository.save(admin);

            String text = "✅ Yangi muddati: " + duration + " kun\n\nYangi tavsifini kiriting (yoki /skip yozing):";
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText(text);
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex139) {
                throw ex139;
            }
        } catch (NumberFormatException e) {
            String text = "❌ Noto'g'ri muddat. Iltimos, raqam kiriting:";
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText(text);
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex140) {
                throw ex140;
            }
            throw e;
        }
    }

    private void handleEditTariffDescription(User admin, String description, Long chatId) {
        String[] tempData = admin.getTempData().split("\\|");
        String tariffId = tempData[0];
        String messageId = tempData[1];
        String name = tempData[2];
        double price = Double.parseDouble(tempData[3]);
        int duration = Integer.parseInt(tempData[4]);
        try {
            SubscriptionType tariff = subscriptionTypeRepository.findById(UUID.fromString(tariffId)).orElse(null);
            if (tariff == null) {
                try {
                    telegramService.sendMessage(new SendMessage(chatId.toString(), "❌ Ta'rif topilmadi."));
                } catch (RuntimeException ex141) {
                    throw ex141;
                }
                return;
            }

            tariff.setName(name);
            tariff.setPrice(price);
            tariff.setDuration(duration);
            tariff.setDescription(description.equals("/skip") || description.isBlank() ? null : description);
            subscriptionTypeRepository.save(tariff);

            admin.setTempData(null);
            admin.setStep(BotStep.NONE);
            userRepository.save(admin);

            StringBuilder sb = new StringBuilder();
            sb.append("✅ Ta'rif muvaffaqiyatli tahrirlandi!\n\n");
            sb.append("🏷️ Nomi: ").append(name).append("\n");
            sb.append("💰 Narx: ").append(price).append(" so'm\n");
            sb.append("⏰ Muddat: ").append(duration).append(" kun\n");
            if (tariff.getDescription() != null) {
                sb.append("📝 Tavsif: ").append(tariff.getDescription()).append("\n");
            }
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText(sb.toString());
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex142) {
                throw ex142;
            }
        } catch (Exception e) {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText("❌ Xatolik yuz berdi: " + e.getMessage());
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex143) {
                throw ex143;
            }
            throw e;
        }
    }

    @Transactional
    public void handleDeleteUser(User admin, String userId, Long chatId, Integer messageId) {
        try {
            User userToDelete = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (userToDelete == null) {
                try {
                    telegramService.sendMessage(new SendMessage(chatId.toString(), "❌ Foydalanuvchi topilmadi."));
                } catch (RuntimeException ex144) {
                    throw ex144;
                }
                return;
            }

            subscriptionRepository.deleteAll(userToDelete.getSubscriptions());
            userToDelete.getSubscriptionTypes().clear();
            userToDelete.getGyms().clear();
            userRepository.delete(userToDelete);

            try {
                telegramService.deleteMessage(chatId.toString(), messageId);
            } catch (RuntimeException ex31) {
                throw ex31;
            }
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Foydalanuvchini o'chirishda xatolik: " + e.getMessage());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex32) {
                throw ex32;
            }
            throw e;
        }
    }

    private void handleEditUser(User admin, String userId, Long chatId, Integer messageId) {
        User userToEdit = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (userToEdit == null) {
            try {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "❌ Foydalanuvchi topilmadi."));
            } catch (RuntimeException ex33) {
                throw ex33;
            }
            return;
        }
        admin.setStep(BotStep.EDIT_USER_NAME);
        admin.setTempData(userId + "|" + messageId);
        userRepository.save(admin);
        String text = "✏️ Foydalanuvchini tahrirlash\n\n" +
                "Hozirgi ismi: " + (userToEdit.getName() != null ? userToEdit.getName() : "-") + "\n" +
                "Yangi ismini kiriting:";
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(text);
        try {
            telegramService.editMessageText(editMessage);
        } catch (RuntimeException ex34) {
            throw ex34;
        }
    }

    private void handleEditUserName(User admin, String name, Long chatId) {
        String[] tempData = admin.getTempData().split("\\|");
        String messageId = tempData[1];
        admin.setStep(BotStep.EDIT_USER_PHONE);
        admin.setTempData(admin.getTempData() + "|" + name);
        userRepository.save(admin);
        User userToEdit = userRepository.findById(UUID.fromString(tempData[0])).orElse(null);
        String text = "✅ Yangi ismi: " + name + "\n\n" +
                "Hozirgi telefon: " + (userToEdit != null && userToEdit.getPhone() != null ? userToEdit.getPhone() : "-") + "\n" +
                "Yangi telefon raqamini kiriting:";
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(Integer.parseInt(messageId));
        editMessage.setText(text);
        try {
            telegramService.editMessageText(editMessage);
        } catch (RuntimeException ex35) {
            throw ex35;
        }
    }

    private void handleEditUserPhone(User admin, String phone, Long chatId) {
        String[] tempData = admin.getTempData().split("\\|");
        String userId = tempData[0];
        String messageId = tempData[1];
        String normalizedPhone = normalizePhoneNumber(phone);
        userRepository.findByPhone(normalizedPhone).ifPresent(existingUser -> {
            if (!existingUser.getId().toString().equals(userId)) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId.toString());
                editMessage.setMessageId(Integer.parseInt(messageId));
                editMessage.setText("❌ Bu telefon raqami allaqachon mavjud. Boshqa raqam kiriting:");
                try {
                    telegramService.editMessageText(editMessage);
                } catch (RuntimeException ex36) {
                    throw ex36;
                }
                return;
            }
        });
        admin.setStep(BotStep.EDIT_USER_SELECT_SUBSCRIPTION_TYPE);
        admin.setTempData(admin.getTempData() + "|" + normalizedPhone);
        userRepository.save(admin);
        showTariffsForSelectionEditUser(admin, chatId, Integer.parseInt(messageId));
    }

    private void showTariffsForSelectionEditUser(User admin, Long chatId, Integer messageId) {
        String[] tempData = admin.getTempData().split("\\|");
        User userToEdit = userRepository.findById(UUID.fromString(tempData[0])).orElse(null);
        List<Gym> adminGyms = admin.getGyms();
        if (adminGyms.isEmpty()) {
            try {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "Sizga gym biriktirilmagan."));
            } catch (RuntimeException e) {
                throw e;
            }
            return;
        }
        List<SubscriptionType> tariffs = subscriptionTypeRepository.findByGym(adminGyms.get(0));
        StringBuilder sb = new StringBuilder();
        sb.append("Foydalanuvchi uchun yangi ta'rifni tanlang:\n\n");
        UUID currentTariffId = null;
        if (userToEdit != null && !userToEdit.getSubscriptionTypes().isEmpty()) {
            currentTariffId = userToEdit.getSubscriptionTypes().get(0).getId();
        }
        if (tariffs.isEmpty()) {
            sb.append("Ta'riflar mavjud emas.");
        } else {
            for (int i = 0; i < tariffs.size(); i++) {
                SubscriptionType tariff = tariffs.get(i);
                boolean isCurrent = currentTariffId != null && currentTariffId.equals(tariff.getId());
                sb.append((i + 1)).append(isCurrent ? ". ✅ " : ". ").append(tariff.getName()).append(" - ").append(tariff.getPrice()).append(" so'm");
                if (isCurrent) sb.append(" (hozirgi)");
                sb.append("\n");
            }
            sb.append("\nTa'rif raqamini kiriting:");
        }
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(sb.toString());
        try {
            telegramService.editMessageText(editMessage);
        } catch (RuntimeException ex37) {
            throw ex37;
        }
    }

    private void handleEditUserTariff(User admin, String messageText, Long chatId) {
        String[] tempData = admin.getTempData().split("\\|");
        String messageId = tempData[1];
        User userToEdit = userRepository.findById(UUID.fromString(tempData[0])).orElse(null);
        List<Gym> adminGyms = admin.getGyms();
        if (adminGyms.isEmpty() || userToEdit == null) {
            try {
                telegramService.sendMessage(new SendMessage(chatId.toString(), "Sizga gym biriktirilmagan yoki foydalanuvchi topilmadi."));
            } catch (RuntimeException e) {
                throw e;
            }
            return;
        }
        List<SubscriptionType> tariffs = subscriptionTypeRepository.findByGym(adminGyms.get(0));
        try {
            int tariffIndex = Integer.parseInt(messageText) - 1;
            if (tariffIndex < 0 || tariffIndex >= tariffs.size()) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId.toString());
                editMessage.setMessageId(Integer.parseInt(messageId));
                editMessage.setText("Noto'g'ri ta'rif tanlandi. Qaytadan urinib ko'ring.");
                try {
                    telegramService.editMessageText(editMessage);
                } catch (RuntimeException ex38) {
                    throw ex38;
                }
                return;
            }
            SubscriptionType selectedTariff = tariffs.get(tariffIndex);
            admin.setStep(BotStep.EDIT_USER_DAY_COUNT);
            admin.setTempData(admin.getTempData() + "|" + selectedTariff.getId());
            userRepository.save(admin);
            String text = "✅ Ta'rif tanlandi: " + selectedTariff.getName() + "\n\n";
            text += "Hozirgi kelgan kunlar: ";
            text += "Foydalanuvchining kelgan kunlar sonini kiriting:";
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText(text);
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex39) {
                throw ex39;
            }
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "Xatolik: " + e.getMessage());
            try {
                telegramService.sendMessage(send);
            } catch (RuntimeException ex40) {
                throw ex40;
            }
            throw e;
        }
    }

    private void handleEditUserDayCount(User admin, String dayCountText, Long chatId) {
        String[] tempData = admin.getTempData().split("\\|");
        String userId = tempData[0];
        String messageId = tempData[1];
        String name = tempData[2];
        String phone = tempData[3];
        String tariffId = tempData[4];
        try {
            int dayCount = Integer.parseInt(dayCountText);
            User userToEdit = userRepository.findById(UUID.fromString(userId)).orElse(null);
            SubscriptionType tariff = subscriptionTypeRepository.findById(UUID.fromString(tariffId)).orElse(null);
            if (userToEdit == null || tariff == null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId.toString());
                editMessage.setMessageId(Integer.parseInt(messageId));
                editMessage.setText("Foydalanuvchi yoki ta'rif topilmadi.");
                try {
                    telegramService.editMessageText(editMessage);
                } catch (RuntimeException ex41) {
                    throw ex41;
                }
                return;
            }
            userToEdit.setName(name);
            userToEdit.setPhone(phone);
            userToEdit.getSubscriptionTypes().clear();
            userToEdit.getSubscriptionTypes().add(tariff);
            userToEdit.getSubscriptions().clear();
            subscriptionService.subscribeUserToSubscriptionType(userToEdit, tariff, tariff.getPrice().intValue(), tariff.getDuration());
            userRepository.save(userToEdit);
            admin.setTempData(null);
            admin.setStep(BotStep.NONE);
            userRepository.save(admin);
            StringBuilder sb = new StringBuilder();
            sb.append("✅ Foydalanuvchi muvaffaqiyatli tahrirlandi!\n\n");
            sb.append("👤 Ism: ").append(name).append("\n");
            sb.append("📱 Telefon: ").append(phone).append("\n");
            sb.append("📊 Ta'rif: ").append(tariff.getName()).append("\n");
            sb.append("📅 Kelgan kunlar: ").append(dayCount).append(" kun");
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText(sb.toString());
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex42) {
                throw ex42;
            }
        } catch (NumberFormatException e) {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText("Noto'g'ri kunlar soni kiritildi.");
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex43) {
                throw ex43;
            }
            throw e;
        } catch (Exception e) {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(Integer.parseInt(messageId));
            editMessage.setText("Xatolik yuz berdi. Qaytadan urinib ko'ring.");
            try {
                telegramService.editMessageText(editMessage);
            } catch (RuntimeException ex44) {
                throw ex44;
            }
            throw e;
        }
    }

    private void handleUserInlineQuery(String query, String queryId, User admin) {
        List<User> users = new ArrayList<>();
        if (admin == null) return;
        if (!query.isEmpty()) {
            String phoneQuery = query.replaceAll("[^0-9]", "");
            users = userRepository.findByPhoneContaining(phoneQuery);
        }
        List<Gym> adminGyms = admin.getGyms();
        List<User> filtered = new ArrayList<>();
        for (User user : users) {
            boolean isAdmin = user.getRoles() != null && user.getRoles().stream().anyMatch(r -> r.getName().contains("ADMIN"));
            if (isAdmin) continue;
            for (Gym gym : user.getGyms()) {
                if (adminGyms.contains(gym)) {
                    Subscription active = user.getSubscriptions().stream().filter(Subscription::isStatus).findFirst().orElse(null);
                    if (active != null) filtered.add(user);
                    break;
                }
            }
        }
        List<InlineQueryResult> results = new ArrayList<>();
        for (User user : filtered) {
            String title = user.getName() + " (" + user.getPhone() + ")";
            InlineQueryResultArticle article = new InlineQueryResultArticle();
            article.setId("incday_" + user.getId());
            article.setTitle(title);
            InputTextMessageContent content = new InputTextMessageContent();
            StringBuilder sb = new StringBuilder();
            sb.append("Ism: ").append(user.getName()).append("\n");
            sb.append("Phone: ").append(user.getPhone()).append("\n");
            sb.append("#id:").append(user.getId());
            content.setMessageText(sb.toString());
            article.setInputMessageContent(content);
            results.add(article);
        }
        AnswerInlineQuery answer = new AnswerInlineQuery();
        answer.setInlineQueryId(queryId);
        answer.setResults(results);
        answer.setCacheTime(1);
        telegramService.answerInlineQuery(answer);
    }
}
