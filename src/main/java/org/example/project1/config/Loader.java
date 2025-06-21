package org.example.project1.config;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.*;
import org.example.project1.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.example.project1.repository.SubscriptionRepository;
import org.example.project1.service.subscriptionservice.SubscriptionService;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class Loader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GymRepository gymRepository;
    private final TariffRepository tariffRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Override
    public void run(String... args) {
        cleanupDuplicatePhones();
        cleanupDuplicateTelegramChatIds();
        initRoles();
        initGyms();
        initTariffs();
        initAdminUsers();
        initRegularUsers();
        assignGymsAndTariffsToUsers();
    }

    private void cleanupDuplicatePhones() {
        try {
            System.out.println("Starting duplicate phone numbers cleanup...");
            
            try {
                userRepository.deleteDuplicateUsers();
                System.out.println("Bulk duplicate cleanup completed.");
            } catch (Exception e) {
                System.out.println("Bulk cleanup failed, falling back to individual cleanup: " + e.getMessage());
                
                List<Object[]> duplicates = userRepository.findDuplicatePhones();
                if (!duplicates.isEmpty()) {
                    System.out.println("Found " + duplicates.size() + " duplicate phone numbers. Cleaning up...");
                    
                    for (Object[] duplicate : duplicates) {
                        String phone = (String) duplicate[0];
                        Long count = (Long) duplicate[1];
                        System.out.println("Phone: " + phone + " has " + count + " duplicates");
                        
                        List<User> usersWithSamePhone = userRepository.findAllByPhone(phone);
                        
                        if (usersWithSamePhone.size() > 1) {
                            User keepUser = usersWithSamePhone.stream()
                                .filter(user -> user.getTelegramChatId() != null)
                                .findFirst()
                                .orElse(usersWithSamePhone.get(0));
                            
                            System.out.println("Keeping user: " + keepUser.getName() + " (ID: " + keepUser.getId() + ")");
                            
                            for (User user : usersWithSamePhone) {
                                if (!user.getId().equals(keepUser.getId())) {
                                    System.out.println("Deleting duplicate user: " + user.getName() + " (ID: " + user.getId() + ") with phone: " + user.getPhone());
                                    userRepository.delete(user);
                                }
                            }
                        }
                    }
                    System.out.println("Individual duplicate phone numbers cleanup completed.");
                } else {
                    System.out.println("No duplicate phone numbers found.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error during duplicate phone cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cleanupDuplicateTelegramChatIds() {
        try {
            System.out.println("Starting duplicate telegram chat IDs cleanup...");
            
            List<User> allUsers = userRepository.findAll();
            Map<Long, List<User>> chatIdGroups = allUsers.stream()
                .filter(user -> user.getTelegramChatId() != null)
                .collect(Collectors.groupingBy(User::getTelegramChatId));
            
            for (Map.Entry<Long, List<User>> entry : chatIdGroups.entrySet()) {
                Long chatId = entry.getKey();
                List<User> usersWithSameChatId = entry.getValue();
                
                if (usersWithSameChatId.size() > 1) {
                    System.out.println("Found " + usersWithSameChatId.size() + " users with same telegram chat ID: " + chatId);
                    
                    User keepUser = usersWithSameChatId.stream()
                        .filter(user -> user.getPhone() != null && !user.getPhone().isEmpty())
                        .findFirst()
                        .orElse(usersWithSameChatId.get(0));
                    
                    System.out.println("Keeping user: " + keepUser.getName() + " (ID: " + keepUser.getId() + ")");
                    
                    for (User user : usersWithSameChatId) {
                        if (!user.getId().equals(keepUser.getId())) {
                            System.out.println("Deleting duplicate user: " + user.getName() + " (ID: " + user.getId() + ") with chat ID: " + chatId);
                            userRepository.delete(user);
                        }
                    }
                }
            }
            System.out.println("Duplicate telegram chat IDs cleanup completed.");
        } catch (Exception e) {
            System.err.println("Error during duplicate telegram chat IDs cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initRoles() {
        List<Role> roles = List.of(
            Role.builder().name("ROLE_USER").build(),
            Role.builder().name("ROLE_ADMIN").build(),
            Role.builder().name("ROLE_SUPERADMIN").build()
        );
        for (Role role : roles) {
            roleRepository.findByName(role.getName()).orElseGet(() -> roleRepository.save(role));
        }
    }

    private void initGyms() {
        if (gymRepository.count() == 0) {
            Gym gym1 = new Gym();
            gym1.setName("PowerGym Tashkent");
            gym1.setLocation("Tashkent, Yunusobod tumani");
            gym1.setMembers(new ArrayList<>());
            gymRepository.save(gym1);

            Gym gym2 = new Gym();
            gym2.setName("FitnessPro Samarkand");
            gym2.setLocation("Samarkand, Registon ko'chasi");
            gym2.setMembers(new ArrayList<>());
            gymRepository.save(gym2);

            Gym gym3 = new Gym();
            gym3.setName("BodyBuilder Gym");
            gym3.setLocation("Tashkent, Chilonzor tumani");
            gym3.setMembers(new ArrayList<>());
            gymRepository.save(gym3);
        }
    }

    private void initTariffs() {
        List<Tariff> tariffs = List.of(
            Tariff.builder().name("Basic Plan").price(150000.0).duration(30).description("Asosiy mashg'ulotlar").users(new ArrayList<>()).build(),
            Tariff.builder().name("Premium Plan").price(250000.0).duration(30).description("Barcha mashg'ulotlar + trener").users(new ArrayList<>()).build(),
            Tariff.builder().name("VIP Plan").price(400000.0).duration(30).description("Individual trener + spa").users(new ArrayList<>()).build(),
            Tariff.builder().name("Student Plan").price(100000.0).duration(30).description("Talabalar uchun chegirma").users(new ArrayList<>()).build(),
            Tariff.builder().name("Family Plan").price(500000.0).duration(30).description("Oila uchun (4 kishi)").users(new ArrayList<>()).build(),
            Tariff.builder().name("Annual Basic").price(1500000.0).duration(365).description("Yillik basic plan").users(new ArrayList<>()).build(),
            Tariff.builder().name("Annual Premium").price(2500000.0).duration(365).description("Yillik premium plan").users(new ArrayList<>()).build()
        );
        for (Tariff tariff : tariffs) {
            if (tariffRepository.findByName(tariff.getName()).isEmpty()) {
                tariffRepository.save(tariff);
            }
        }
    }

    private void initAdminUsers() {
        final String superAdminPhone = "+998883054343";
        final String adminPhone = "+998907110709";
        Role superAdminRole = roleRepository.findByName("ROLE_SUPERADMIN").orElse(null);
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElse(null);
        List<Gym> gyms = gymRepository.findAll();

        if (userRepository.findByPhone(superAdminPhone).isEmpty()) {
            User superAdmin = new User();
            superAdmin.setName("Mirshod Hojiyev");
            superAdmin.setPhone(superAdminPhone);
            superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setRoles(List.of(superAdminRole));
            superAdmin.setPurchaseDate(new Date());
            superAdmin.setGyms(new ArrayList<>());
            superAdmin.setTariffs(new ArrayList<>());
            userRepository.save(superAdmin);
        }

        if (userRepository.findByPhone(adminPhone).isEmpty()) {
            User admin = new User();
            admin.setName("Shaxzod");
            admin.setPhone(adminPhone);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(List.of(adminRole));
            admin.setPurchaseDate(new Date());
            if (!gyms.isEmpty()) {
                admin.setGyms(List.of(gyms.get(0)));
            } else {
                admin.setGyms(new ArrayList<>());
            }
            admin.setTariffs(new ArrayList<>());
            userRepository.save(admin);
        }
    }

    private void initRegularUsers() {
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        List<User> users = List.of(
            User.builder().name("Alisher").phone("+998901111111").password(passwordEncoder.encode("alisher123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build(),
            User.builder().name("Malika Tosheva").phone("+998902222222").password(passwordEncoder.encode("malika123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build(),
            User.builder().name("Bobur Rahimov").phone("+998903333333").password(passwordEncoder.encode("bobur123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build(),
            User.builder().name("Nilufar Saidova").phone("+998904444444").password(passwordEncoder.encode("nilufar123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build(),
            User.builder().name("Jasur Abdullayev").phone("+998905555555").password(passwordEncoder.encode("jasur123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build(),
            User.builder().name("Sevara Nazarova").phone("+998906666666").password(passwordEncoder.encode("sevara123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build(),
            User.builder().name("Otabek Yusupov").phone("+998907777777").password(passwordEncoder.encode("otabek123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build(),
            User.builder().name("Madina Qodirova").phone("+998908888888").password(passwordEncoder.encode("madina123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build()
        );
        for (User user : users) {
            if (userRepository.findByPhone(user.getPhone()).isEmpty()) {
                userRepository.save(user);
            }
        }
    }

    private void assignGymsAndTariffsToUsers() {
        List<Gym> gyms = gymRepository.findAll();
        List<Tariff> tariffs = tariffRepository.findAll();
        List<User> users = userRepository.findAll();
        if (gyms.isEmpty() || users.isEmpty() || tariffs.isEmpty()) return;
        int gymCount = gyms.size();
        int tariffCount = tariffs.size();
        int idx = 0;
        for (User user : users) {
            boolean isAdmin = user.getRoles() != null && user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN") || r.getName().equals("ROLE_SUPERADMIN"));
            if (isAdmin) continue;
            if (user.getGyms() == null || user.getGyms().isEmpty()) {
                Gym gym = gyms.get(idx % gymCount);
                user.setGyms(List.of(gym));
            }
            if (user.getTariffs() == null || user.getTariffs().isEmpty()) {
                Tariff tariff = tariffs.get(idx % tariffCount);
                user.setTariffs(List.of(tariff));
                subscriptionService.subscribeUserToTariff(user, tariff, tariff.getPrice().intValue(), tariff.getDuration());
            }
            userRepository.save(user);
            idx++;
        }
    }
}
