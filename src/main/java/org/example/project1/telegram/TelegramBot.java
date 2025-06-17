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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TelegramBot {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TariffRepository tariffRepository;
    private final GymRepository gymRepository;
    private final TelegramServiceImpl telegramService;
    private final PasswordEncoder passwordEncoder;

    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        createRolesIfNotExist();

        boolean isStart = message.hasText() && message.getText().equals("/start");
        if (isStart) {
            userRepository.findByTelegramChatIdWithRoles(chatId).ifPresent(existingUser -> {
                existingUser.setStep(null);
                existingUser.setTelegramChatId(null);
                userRepository.save(existingUser);
            });
            
            SendMessage send = new SendMessage(chatId.toString(), "Iltimos, telefon raqamingizni yuboring.");
            send.setReplyMarkup(phoneButton());
            telegramService.sendMessage(send);
            return;
        }

        if (message.hasContact()) {
            String phone = message.getContact().getPhoneNumber();
            User dbUser = userRepository.findByPhone(phone).orElse(null);
            if (dbUser != null) {
                dbUser.setTelegramChatId(chatId);
                dbUser.setStep(BotStep.NONE);
                userRepository.save(dbUser);
                
                StringBuilder userInfo = new StringBuilder();
                userInfo.append("✅ Siz muvaffaqiyatli ro'yxatdan o'tdingiz!\n\n");
                userInfo.append("👤 Foydalanuvchi ma'lumotlari:\n");
                userInfo.append("📱 Telefon: ").append(dbUser.getPhone()).append("\n");
                if (dbUser.getRoles() != null && !dbUser.getRoles().isEmpty()) {
                    userInfo.append("🔑 Role: ").append(dbUser.getRoles().get(0).getName()).append("\n");
                }
                if (!dbUser.getTariffs().isEmpty()) {
                    Tariff userTariff = dbUser.getTariffs().get(0);
                    userInfo.append("📊 Ta'rif: ").append(userTariff.getName()).append("\n");
                    userInfo.append("💰 Narx: ").append(userTariff.getPrice()).append(" so'm\n");
                    userInfo.append("⏰ Muddat: ").append(userTariff.getDuration()).append(" kun\n");
                }

                SendMessage send = new SendMessage(chatId.toString(), userInfo.toString());
                send.setReplyMarkup(adminKeyboard());
                telegramService.sendMessage(send);
                return;
            } else {
                SendMessage send = new SendMessage(chatId.toString(), "Bu telefon raqami ro'yxatdan o'tmagan.");
                telegramService.sendMessage(send);
                return;
            }
        }

        User user = userRepository.findByTelegramChatIdWithRoles(chatId).orElse(null);
        if (user == null) {
            SendMessage send = new SendMessage(chatId.toString(), "Iltimos, telefon raqamingizni yuboring.");
            send.setReplyMarkup(phoneButton());
            telegramService.sendMessage(send);
            return;
        }

        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN") || r.getName().equals("ROLE_SUPERADMIN"));

        if (!isAdmin) {
            SendMessage send = new SendMessage(chatId.toString(), "Sizda admin huquqlari mavjud emas.");
            telegramService.sendMessage(send);
            return;
        }

        if (message.hasText()) {
            String messageText = message.getText();
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
                    user.setStep(BotStep.NONE);
                    userRepository.save(user);
                    SendMessage send = new SendMessage(chatId.toString(), "Bosh menyu");
                    send.setReplyMarkup(adminKeyboard());
                    telegramService.sendMessage(send);
                    return;
            }
        }

        if (user.getStep() != null && user.getStep() != BotStep.NONE) {
            handleUserSteps(user, message);
        }
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
                if (!messageText.isBlank()) {
                    handleAddUserName(user, messageText, chatId);
                } else {
                    SendMessage send = new SendMessage(chatId.toString(), "Iltimos, foydalanuvchi ismini kiriting:");
                    telegramService.sendMessage(send);
                }
                return true;

            case ADD_USER_PHONE:
                if (message.hasContact()) {
                    handleAddUserPhone(user, message.getContact().getPhoneNumber(), chatId);
                } else if (messageText.matches("\\+?[0-9]{9,15}")) {
                    handleAddUserPhone(user, messageText, chatId);
                } else {
                    SendMessage send = new SendMessage(chatId.toString(), "Iltimos, to'g'ri telefon raqam kiriting yoki kontakt yuboring:");
                    telegramService.sendMessage(send);
                }
                return true;

            case ADD_USER_SELECT_TARIFF:
                handleAddUserTariff(user, messageText, chatId);
                return true;

            case ADD_USER_GYM:
                handleAddUserGym(user, messageText, chatId);
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
                "✅ Ism: " + name + "\n\nEndi telefon raqamini kiriting yoki yuboring:");
        send.setReplyMarkup(phoneButton());
        telegramService.sendMessage(send);
    }

    private void handleAddUserPhone(User admin, String phone, Long chatId) {
        String userName = admin.getTempData();
        User newUser = new User();
        newUser.setPhone(phone);
        newUser.setName(userName);
        newUser.setPassword(passwordEncoder.encode(phone));
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

    private void handleAddUserTariff(User admin, String messageText, Long chatId) {
        try {
            UUID userId = UUID.fromString(admin.getTempData());
            User newUser = userRepository.findById(userId).orElse(null);

            if (newUser == null) {
                SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik yuz berdi. Qaytadan urinib ko'ring.");
                telegramService.sendMessage(send);
                return;
            }

            try {
                int tariffIndex = Integer.parseInt(messageText) - 1;
                List<Tariff> tariffs = tariffRepository.findAll();

                if (tariffIndex >= 0 && tariffIndex < tariffs.size()) {
                    Tariff selectedTariff = tariffs.get(tariffIndex);
                    newUser.getTariffs().add(selectedTariff);
                    userRepository.save(newUser);

                    admin.setStep(BotStep.ADD_USER_GYM);
                    userRepository.save(admin);

                    showGymsForSelection(chatId);
                } else {
                    SendMessage send = new SendMessage(chatId.toString(), "❌ Noto'g'ri raqam. Qaytadan urinib ko'ring:");
                    telegramService.sendMessage(send);
                }
            } catch (NumberFormatException e) {
                SendMessage send = new SendMessage(chatId.toString(), "❌ Noto'g'ri raqam. Qaytadan urinib ko'ring:");
                telegramService.sendMessage(send);
            }
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik yuz berdi: " + e.getMessage());
            telegramService.sendMessage(send);
        }
    }

    private void showGymsForSelection(Long chatId) {
        List<Gym> gyms = gymRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("🏋️ Gym tanlang:\n\n");

        if (gyms.isEmpty()) {
            sb.append("Gym'lar mavjud emas. /skip yozing davom etish uchun.");
        } else {
            for (int i = 0; i < gyms.size(); i++) {
                Gym gym = gyms.get(i);
                sb.append((i + 1)).append(". ").append(gym.getName());
                sb.append(" - ").append(gym.getLocation()).append("\n");
            }
            sb.append("\nGym raqamini kiriting yoki /skip yozing:");
        }

        SendMessage send = new SendMessage(chatId.toString(), sb.toString());
        telegramService.sendMessage(send);
    }

    private void handleAddUserGym(User admin, String messageText, Long chatId) {
        try {
            UUID userId = UUID.fromString(admin.getTempData());
            User newUser = userRepository.findById(userId).orElse(null);
            if (newUser == null) {
                SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik yuz berdi. Qaytadan urinib ko'ring.");
                telegramService.sendMessage(send);
                return;
            }
            if (!messageText.equals("/skip")) {
                try {
                    int gymIndex = Integer.parseInt(messageText) - 1;
                    List<Gym> gyms = gymRepository.findAll();
                    if (gymIndex >= 0 && gymIndex < gyms.size()) {
                        Gym selectedGym = gyms.get(gymIndex);
                        newUser.addGym(selectedGym);
                        userRepository.save(newUser);
                    } else {
                        SendMessage send = new SendMessage(chatId.toString(), "❌ Noto'g'ri raqam. Qaytadan urinib ko'ring:");
                        telegramService.sendMessage(send);
                        return;
                    }
                } catch (NumberFormatException e) {
                    SendMessage send = new SendMessage(chatId.toString(), "❌ Noto'g'ri raqam. Qaytadan urinib ko'ring:");
                    telegramService.sendMessage(send);
                    return;
                }
            }
            admin.setTempData(null);
            admin.setStep(BotStep.NONE);
            userRepository.save(admin);
            StringBuilder sb = new StringBuilder();
            sb.append("✅ Foydalanuvchi muvaffaqiyatli qo'shildi!\n");
            sb.append("👤 Ism: ").append(newUser.getName()).append("\n");
            sb.append("📱 Telefon: ").append(newUser.getPhone()).append("\n");
            sb.append("🔐 Parol: ").append(newUser.getPhone()).append("\n");
            if (!newUser.getTariffs().isEmpty()) {
                Tariff t = newUser.getTariffs().get(0);
                sb.append("📊 Ta'rif: ").append(t.getName()).append("\n");
                sb.append("💰 Narx: ").append(t.getPrice()).append(" so'm\n");
                sb.append("⏰ Muddat: ").append(t.getDuration()).append(" kun\n");
                if (t.getDescription() != null) {
                    sb.append("📝 Tavsif: ").append(t.getDescription()).append("\n");
                }
            }
            if (!newUser.getGyms().isEmpty()) {
                sb.append("🏋️ Gym: ").append(newUser.getGyms().get(0).getName()).append("\n");
            }
            SendMessage send = new SendMessage(chatId.toString(), sb.toString());
            send.setReplyMarkup(adminKeyboard());
            telegramService.sendMessage(send);
        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(), "❌ Xatolik yuz berdi: " + e.getMessage());
            telegramService.sendMessage(send);
        }
    }

    private void showReportOptions(User admin, Long chatId) {
        admin.setStep(BotStep.WAITING_FOR_REPORT_TYPE);
        userRepository.save(admin);

        StringBuilder sb = new StringBuilder();
        sb.append("📈 Hisobot turlari:\n\n");
        sb.append("1. Barcha foydalanuvchilar\n");
        sb.append("2. Admin foydalanuvchilar\n");
        sb.append("3. Ta'riflar bo'yicha hisobot\n");
        sb.append("4. Gym'lar bo'yicha hisobot\n");
        sb.append("\nKerakli hisobot raqamini kiriting:");

        SendMessage send = new SendMessage(chatId.toString(), sb.toString());
        telegramService.sendMessage(send);
    }

    @Transactional
    protected void handleReportRequest(User admin, String messageText, Long chatId) {
        admin.setStep(BotStep.NONE);
        userRepository.save(admin);

        StringBuilder sb = new StringBuilder();

        switch (messageText) {
            case "1":
                sb.append("👥 Barcha foydalanuvchilar hisoboti:\n\n");
                List<User> allUsers = userRepository.findAll();
                sb.append("Jami foydalanuvchilar: ").append(allUsers.size()).append("\n\n");

                for (User user : allUsers) {
                    sb.append("📱 ").append(user.getPhone() != null ? user.getPhone() : "N/A").append("\n");
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        sb.append("👤 Role: ").append(user.getRoles().get(0).getName()).append("\n");
                    }
                    if (!user.getTariffs().isEmpty()) {
                        sb.append("📊 Ta'rif: ").append(user.getTariffs().get(0).getName()).append("\n");
                    }
                    if (!user.getGyms().isEmpty()) {
                        sb.append("🏋️ Gym: ").append(user.getGyms().get(0).getName()).append("\n");
                    }
                    sb.append("─────────────────\n");
                }
                break;

            case "2":
                sb.append("👑 Admin foydalanuvchilar:\n\n");
                List<User> admins = userRepository.findByRoleName("ROLE_ADMIN");
                List<User> superAdmins = userRepository.findByRoleName("ROLE_SUPERADMIN");

                sb.append("Admin'lar: ").append(admins.size()).append("\n");
                sb.append("Super Admin'lar: ").append(superAdmins.size()).append("\n\n");

                admins.forEach(admin1 -> sb.append("👤 ").append(admin1.getPhone()).append(" (ADMIN)\n"));
                superAdmins.forEach(superAdmin -> sb.append("👤 ").append(superAdmin.getPhone()).append(" (SUPERADMIN)\n"));
                break;

            case "3":
                sb.append("📊 Ta'riflar hisoboti:\n\n");
                List<Tariff> tariffs = tariffRepository.findAll();
                sb.append("Jami ta'riflar: ").append(tariffs.size()).append("\n\n");

                for (Tariff tariff : tariffs) {
                    sb.append("🏷️ ").append(tariff.getName()).append("\n");
                    sb.append("💰 ").append(tariff.getPrice()).append(" so'm\n");
                    sb.append("👥 Foydalanuvchilar: ").append(tariff.getUsers().size()).append("\n");
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
            telegramService.sendMessage(send);

        } catch (Exception e) {
            SendMessage send = new SendMessage(chatId.toString(),
                    "❌ Xatolik yuz berdi: " + e.getMessage());
            telegramService.sendMessage(send);
        }
    }
}
