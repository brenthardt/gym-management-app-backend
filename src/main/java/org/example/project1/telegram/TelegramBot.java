package org.example.project1.telegram;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.BotStep;
import org.example.project1.entity.Role;
import org.example.project1.entity.Tariff;
import org.example.project1.entity.User;
import org.example.project1.entity.Gym;
import org.example.project1.repository.RoleRepository;
import org.example.project1.repository.TariffRepository;
import org.example.project1.repository.UserRepository;
import org.example.project1.repository.GymRepository;
import org.example.project1.service.telegramservice.TelegramServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.project1.service.subscriptionservice.SubscriptionService;

@Component
@RequiredArgsConstructor
public class TelegramBot {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TariffRepository tariffRepository;
    private final GymRepository gymRepository;
    private final TelegramServiceImpl telegramService;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;

    @Transactional
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getMessage().getChatId();
            String data = callbackQuery.getData();
            
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
            telegramService.sendMessage(send);
            return;
        }
        if (message.hasContact()) {
            String phone = normalizePhoneNumber(message.getContact().getPhoneNumber());
            User dbUser = userRepository.findFirstByPhone(phone).orElse(null);
            if (dbUser != null) {
                dbUser.setTelegramChatId(chatId);
                dbUser.setStep(BotStep.NONE);
                userRepository.save(dbUser);
                StringBuilder userInfo = new StringBuilder();
                userInfo.append("✅ Siz muvaffaqiyatli ro'yxatdan o'tdingiz!\n\n");
                userInfo.append("👤 Foydalanuvchi ma'lumotlari:\n");
                userInfo.append("📱 Telefon: ").append(dbUser.getPhone()).append("\n");
                String roleName = dbUser.getRoles() != null && !dbUser.getRoles().isEmpty() ? dbUser.getRoles().get(0).getName() : "";
                userInfo.append("🔑 Role: ").append(roleName).append("\n");
                if (roleName.equals("ROLE_USER")) {
                    if (!dbUser.getGyms().isEmpty()) {
                        Gym gym = dbUser.getGyms().get(0);
                        userInfo.append("🏋️ Gym: ").append(gym.getName()).append(" (" + gym.getLocation() + ")\n");
                    } else {
                        userInfo.append("🏋️ Gym: Biriktirilmagan\n");
                    }
                    if (!dbUser.getTariffs().isEmpty()) {
                        Tariff userTariff = dbUser.getTariffs().get(0);
                        userInfo.append("📊 Ta'rif: ").append(userTariff.getName()).append("\n");
                        userInfo.append("💰 Narx: ").append(userTariff.getPrice()).append(" so'm\n");
                        userInfo.append("⏰ Muddat: ").append(userTariff.getDuration()).append(" kun\n");
                    } else {
                        userInfo.append("📊 Ta'rif: Biriktirilmagan\n");
                    }
                } else if (!dbUser.getTariffs().isEmpty()) {
                    Tariff userTariff = dbUser.getTariffs().get(0);
                    userInfo.append("📊 Ta'rif: ").append(userTariff.getName()).append("\n");
                    userInfo.append("💰 Narx: ").append(userTariff.getPrice()).append(" so'm\n");
                    userInfo.append("⏰ Muddat: ").append(userTariff.getDuration()).append(" kun\n");
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
                telegramService.sendMessage(send);
                return;
            }
        }
        
        List<User> usersWithChatId = userRepository.findAllByTelegramChatId(chatId);
        User user = null;
        
        if (usersWithChatId.isEmpty()) {
            SendMessage send = new SendMessage(chatId.toString(), "Iltimos, telefon raqamingizni yuboring.");
            send.setReplyMarkup(phoneButton());
            telegramService.sendMessage(send);
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
            telegramService.sendMessage(send);
            return;
        }
        
        String roleName = user.getRoles() != null && !user.getRoles().isEmpty() ? user.getRoles().get(0).getName() : "";
        if (roleName.equals("ROLE_USER")) {
            SendMessage send = new SendMessage(chatId.toString(), "Sizda admin huquqlari mavjud emas.");
            telegramService.sendMessage(send);
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
                String messageText = message.getText();
                
                if (messageText.equals("👤 Add User") || messageText.equals("➕ Ta'rif qo'shish") || 
                    messageText.equals("📈 Hisobot") || messageText.equals("🏠 Bosh menyu")) {
                    
                    if (user.getStep() != null && user.getStep() != BotStep.NONE) {
                        user.setStep(BotStep.NONE);
                        user.setTempData(null);
                        userRepository.save(user);
                    }
                    
                    switch (messageText) {
                        case "👤 Add User":
                            startAddUser(user, chatId);
                            return;
                        case "➕ Ta'rif qo'shish":
                            startAddTariff(user, chatId);
                            return;
                        case "📈 Hisobot":
                            showReportOptions(user, chatId);
                            return;
                        case "🏠 Bosh menyu":
                            SendMessage send = new SendMessage(chatId.toString(), "Bosh menyu");
                            send.setReplyMarkup(adminKeyboard());
                            telegramService.sendMessage(send);
                            return;
                    }
                }
            }
            
            if (user.getStep() != null && user.getStep() != BotStep.NONE) {
                if (handleUserSteps(user, message)) {
                    return;
                }
            }
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

    private void requestPassword(Long chatId) {
        SendMessage send = new SendMessage(chatId.toString(), "Iltimos, parolni matn ko'rinishida kiriting (kamida 4 ta belgi) va '/' bilan boshlanmasin:");
        telegramService.sendMessage(send);
    }

    private boolean handleUserSteps(User user, Message message) {
        String messageText = message.hasText() ? message.getText() : "";
        Long chatId = message.getChatId();

        switch (user.getStep()) {
            case ADD_USER_NAME:
                if (!messageText.isBlank() && messageText.trim().length() >= 2) {
                    handleAddUserName(user, messageText.trim(), chatId);
                } else {
                    SendMessage send = new SendMessage(chatId.toString(), "❌ Iltimos, foydalanuvchi ismini kiriting (kamida 2 ta harf):");
                    telegramService.sendMessage(send);
                }
                return true;

            case ADD_USER_PHONE:
                String normalizedPhone = normalizePhoneNumber(messageText);
                if (normalizedPhone != null && normalizedPhone.matches("\\+998[0-9]{9}")) {
                    handleAddUserPhone(user, messageText, chatId);
                } else {
                    SendMessage send = new SendMessage(chatId.toString(), "❌ Iltimos, to'g'ri telefon raqam kiriting (+998xxxxxxxxx, 998xxxxxxxxx, 8xxxxxxxxx yoki 0xxxxxxxxx ko'rinishida):");
                    telegramService.sendMessage(send);
                }
                return true;

            case ADD_USER_SELECT_TARIFF:
                handleAddUserTariff(user, messageText, chatId);
                return true;

            case ADD_TARIFF_NAME:
                handleAddTariffName(user, messageText, chatId);
                return true;

            case ADD_TARIFF_PRICE:
                handleAddTariffPrice(user, messageText, chatId);
                return true;

            case ADD_TARIFF_DURATION:
                handleAddTariffDuration(user, messageText, chatId);
                return true;

            case ADD_TARIFF_DESCRIPTION:
                handleAddTariffDescription(user, messageText, chatId);
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
        row2.add(new KeyboardButton("📈 Hisobot"));
        row2.add(new KeyboardButton("🏠 Bosh menyu"));

        keyboard.add(row1);
        keyboard.add(row2);

        markup.setKeyboard(keyboard);
        return markup;
    }

    private void showTariffs(Long chatId) {
        List<Tariff> tariffs = tariffRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Mavjud ta'riflar:\n\n");

        if (tariffs.isEmpty()) {
            sb.append("Hozircha ta'riflar mavjud emas.");
        } else {
            for (Tariff tariff : tariffs) {
                sb.append("🏷️ ").append(tariff.getName()).append("\n");
                sb.append("💰 Narx: ").append(tariff.getPrice()).append(" so'm\n");
                sb.append("⏰ Muddat: ").append(tariff.getDuration()).append(" kun\n");
                if (tariff.getDescription() != null) {
                    sb.append("📝 Tavsif: ").append(tariff.getDescription()).append("\n");
                }
                if (tariff.getGym() != null) {
                    sb.append("🏋️ Gym: ").append(tariff.getGym().getName()).append("\n");
                }
                sb.append("👥 Foydalanuvchilar soni: ").append(tariff.getUsers().size()).append("\n");
                sb.append("─────────────────\n");
            }
        }

        SendMessage send = new SendMessage(chatId.toString(), sb.toString());
        telegramService.sendMessage(send);
    }

    private void startAddUser(User admin, Long chatId) {
        admin.setStep(BotStep.ADD_USER_NAME);
        userRepository.save(admin);

        SendMessage send = new SendMessage(chatId.toString(),
                "👤 Yangi foydalanuvchi qo'shish\n\nFoydalanuvchining ismini kiriting:");
        telegramService.sendMessage(send);
    }

    private void handleAddUserName(User admin, String name, Long chatId) {
        admin.setStep(BotStep.ADD_USER_PHONE);
        admin.setTempData(name);
        userRepository.save(admin);
        SendMessage send = new SendMessage(chatId.toString(),
                "✅ Ism: " + name + "\n\nEndi telefon raqamini matn ko'rinishida kiriting:");
        telegramService.sendMessage(send);
    }

    private void handleAddUserPhone(User admin, String phone, Long chatId) {
        String normalizedPhone = normalizePhoneNumber(phone);
        if (userRepository.findFirstByPhone(normalizedPhone).isPresent()) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Bu telefon raqami allaqachon mavjud. Iltimos, boshqa raqam kiriting.");
            telegramService.sendMessage(send);
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
        admin.setStep(BotStep.ADD_USER_SELECT_TARIFF);
        userRepository.save(admin);
        showTariffsForSelection(chatId);
    }

    private void showTariffsForSelection(Long chatId) {
        List<Tariff> tariffs = tariffRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Ta'rifni tanlang:\n\n");

        if (tariffs.isEmpty()) {
            sb.append("Ta'riflar mavjud emas.");
        } else {
            for (int i = 0; i < tariffs.size(); i++) {
                Tariff tariff = tariffs.get(i);
                sb.append((i + 1)).append(". ").append(tariff.getName());
                sb.append(" - ").append(tariff.getPrice()).append(" so'm\n");
                sb.append("   ⏰ Muddat: ").append(tariff.getDuration()).append(" kun\n");
            }
            sb.append("\nTa'rif raqamini kiriting:");
        }

        SendMessage send = new SendMessage(chatId.toString(), sb.toString());
        telegramService.sendMessage(send);
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
        telegramService.sendMessage(send);
    }

    @Transactional
    protected void handleReportRequest(User admin, String messageText, Long chatId) {
        admin.setStep(BotStep.NONE);
        userRepository.save(admin);

        String roleName = admin.getRoles() != null && !admin.getRoles().isEmpty() ? admin.getRoles().get(0).getName() : "";
        if (roleName.equals("ROLE_SUPERADMIN")) {
            SendMessage send = new SendMessage(chatId.toString(), "🦸 SuperAdmin muvaffaqiyatli ro'yxatdan o'tdi.");
            telegramService.sendMessage(send);
            return;
        }

        StringBuilder sb = new StringBuilder();
        switch (messageText) {
            case "1":
                List<Gym> adminGyms = userRepository.findAdminGymsWithMembers(admin.getId());
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
                List<Tariff> tariffs = tariffRepository.findAll();
                for (Tariff tariff : tariffs) {
                    sb.append("🏷️ ").append(tariff.getName()).append("\n");
                    sb.append("💰 ").append(tariff.getPrice()).append(" so'm\n");
                    int userCount = (tariff.getUsers() == null) ? 0 : tariff.getUsers().size();
                    sb.append("👥 Foydalanuvchilar: ").append(userCount).append("\n");
                    sb.append("─────────────────\n");
                }
                break;
            case "4":
                sb.append("🏋️ Gym'lar hisoboti:\n\n");
                List<Gym> gyms = gymRepository.findAllWithTariffs();
                sb.append("Jami gym'lar: ").append(gyms.size()).append("\n\n");
                for (int i = 0; i < gyms.size(); i++) {
                    Gym gym = gyms.get(i);
                    sb.append((i + 1)).append(". 🏋️ ").append(gym.getName()).append("\n");
                    sb.append("   📍 Manzil: ").append(gym.getLocation()).append("\n");
                    if (gym.getTariffs() != null && !gym.getTariffs().isEmpty()) {
                        sb.append("   📊 Ta'riflar soni: ").append(gym.getTariffs().size()).append("\n");
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
        telegramService.sendMessage(send);
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
        admin.setStep(BotStep.ADD_TARIFF_NAME);
        userRepository.save(admin);

        SendMessage send = new SendMessage(chatId.toString(),
                "➕ Yangi ta'rif qo'shish\n\nTa'rif nomini kiriting:");
        telegramService.sendMessage(send);
    }

    private void handleAddTariffName(User admin, String name, Long chatId) {
        admin.setStep(BotStep.ADD_TARIFF_PRICE);
        admin.setTempData(name);
        userRepository.save(admin);

        SendMessage send = new SendMessage(chatId.toString(),
                "✅ Ta'rif nomi: " + name + "\n\nTa'rif narxini kiriting (so'm):");
        telegramService.sendMessage(send);
    }

    private void handleAddTariffPrice(User admin, String priceText, Long chatId) {
        try {
            double price = Double.parseDouble(priceText);
            admin.setStep(BotStep.ADD_TARIFF_DURATION);
            admin.setTempData(admin.getTempData() + "|" + price);
            userRepository.save(admin);
            SendMessage send = new SendMessage(chatId.toString(),
                    "✅ Narx: " + price + " so'm\n\nTa'rif muddatini kiriting (kun):");
            telegramService.sendMessage(send);
        } catch (NumberFormatException e) {
            SendMessage send = new SendMessage(chatId.toString(),
                    "❌ Noto'g'ri narx. Iltimos, raqam kiriting:");
            telegramService.sendMessage(send);
        }
    }

    private void handleAddTariffDuration(User admin, String durationText, Long chatId) {
        try {
            int duration = Integer.parseInt(durationText);
            admin.setStep(BotStep.ADD_TARIFF_DESCRIPTION);
            admin.setTempData(admin.getTempData() + "|" + duration);
            userRepository.save(admin);

            SendMessage send = new SendMessage(chatId.toString(),
                    "✅ Muddat: " + duration + " kun\n\nTa'rif tavsifini kiriting (yoki /skip yozing):");
            telegramService.sendMessage(send);
        } catch (NumberFormatException e) {
            SendMessage send = new SendMessage(chatId.toString(),
                    "❌ Noto'g'ri muddat. Iltimos, raqam kiriting:");
            telegramService.sendMessage(send);
        }
    }

    private void handleAddTariffDescription(User admin, String description, Long chatId) {
        try {
            String[] tempData = admin.getTempData().split("\\|");
            String name = tempData[0];
            double price = Double.parseDouble(tempData[1]);
            int duration = Integer.parseInt(tempData[2]);
            Tariff newTariff = new Tariff();
            newTariff.setName(name);
            newTariff.setPrice(price);
            newTariff.setDuration(duration);
            if (!description.equals("/skip") && !description.isBlank()) {
                newTariff.setDescription(description);
            }
            tariffRepository.save(newTariff);
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
            telegramService.sendMessage(send);
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik yuz berdi: " + e.getMessage());
            telegramService.sendMessage(send);
        }
    }

    private void handleAddUserTariff(User admin, String messageText, Long chatId) {
        List<Tariff> tariffs = tariffRepository.findAll();
        try {
            int tariffIndex = Integer.parseInt(messageText) - 1;
            if (tariffIndex < 0 || tariffIndex >= tariffs.size()) {
                SendMessage send = new SendMessage(chatId.toString(), "Noto'g'ri ta'rif tanlandi. Qaytadan urinib ko'ring.");
                telegramService.sendMessage(send);
                return;
            }
            Tariff selectedTariff = tariffs.get(tariffIndex);
            String userIdStr = admin.getTempData();
            if (userIdStr == null) {
                SendMessage send = new SendMessage(chatId.toString(), "Foydalanuvchi topilmadi. Qaytadan urinib ko'ring.");
                telegramService.sendMessage(send);
                return;
            }
            User userToAdd = userRepository.findById(UUID.fromString(userIdStr)).orElse(null);
            if (userToAdd == null) {
                SendMessage send = new SendMessage(chatId.toString(), "Foydalanuvchi topilmadi. Qaytadan urinib ko'ring.");
                telegramService.sendMessage(send);
                return;
            }
            if (userToAdd.getTariffs() == null) userToAdd.setTariffs(new ArrayList<>());
            if (userToAdd.getGyms() == null) userToAdd.setGyms(new ArrayList<>());
            userToAdd.getTariffs().add(selectedTariff);
            List<Gym> adminGyms = admin.getGyms();
            if (adminGyms != null && !adminGyms.isEmpty()) {
                userToAdd.getGyms().add(adminGyms.get(0));
            }
            userToAdd.setStep(BotStep.NONE);
            userRepository.save(userToAdd);
            try {
                subscriptionService.subscribeUserToTariff(userToAdd, selectedTariff, selectedTariff.getPrice().intValue(), selectedTariff.getDuration());
            } catch (Exception e) {
                System.err.println("❌ Error creating subscription: " + e.getMessage());
            }
            StringBuilder successMessage = new StringBuilder();
            successMessage.append("✅ Foydalanuvchi muvaffaqiyatli qo'shildi!\n\n");
            successMessage.append("👤 Ism: ").append(userToAdd.getName()).append("\n");
            successMessage.append("📱 Telefon: ").append(userToAdd.getPhone()).append("\n");
            successMessage.append("📊 Ta'rif: ").append(selectedTariff.getName()).append("\n");
            if (adminGyms != null && !adminGyms.isEmpty()) {
                successMessage.append("🏋️ Gym: ").append(adminGyms.get(0).getName()).append("\n");
            }
            admin.setTempData(null);
            userRepository.save(admin);
            SendMessage send = new SendMessage(chatId.toString(), successMessage.toString());
            send.setReplyMarkup(adminKeyboard());
            telegramService.sendMessage(send);
        } catch (IllegalArgumentException e) {
            SendMessage send = new SendMessage(chatId.toString(), "Noto'g'ri ta'rif tanlandi yoki foydalanuvchi topilmadi. Qaytadan urinib ko'ring.");
            telegramService.sendMessage(send);
        }
    }

    @Transactional
    protected void cleanupDuplicateUsers(Long chatId) {
        try {
            userRepository.clearSubscriptionForDuplicateUsers(chatId);
            userRepository.deleteSubscriptionsForDuplicateUsers(chatId);
            userRepository.deleteAllSubscriptionsForDuplicateUsers();
            userRepository.deleteDuplicateTelegramChatIdUsers();
        } catch (Exception e) {
            System.err.println("❌ Error cleaning up duplicate users for chat ID " + chatId + ": " + e.getMessage());
        }
    }
}
