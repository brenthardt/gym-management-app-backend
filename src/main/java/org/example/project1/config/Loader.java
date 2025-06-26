package org.example.project1.config;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.*;
import org.example.project1.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.example.project1.repository.SubscriptionRepository;
import org.example.project1.service.subscriptionservice.SubscriptionService;
import org.example.project1.repository.SubscriptionTypeRepository;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class Loader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionTypeRepository subscriptionTypeRepository;

    @Override
    public void run(String... args) {
        cleanupDuplicatePhones();
        cleanupDuplicateTelegramChatIds();
        initRoles();
        initGyms();
        initAdminUsers();
        initRegularUsers();
        assignGymsAndSubscriptionTypesToUsers();
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

    private void initAdminUsers() {
        final String superAdminPhone = "+998883054343"; // Mirshod Hojiyev
        final String adminPhone = "+998907110709"; // Shaxzod
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
            superAdmin.setSubscriptionTypes(new ArrayList<>());
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
            admin.setSubscriptionTypes(new ArrayList<>());
            userRepository.save(admin);
        }
    }

    private void initRegularUsers() {
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        Random random = new Random();
        List<User> users = List.of(
            User.builder().name("Alisher").phone("+998901111111").password(passwordEncoder.encode("alisher123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).subscriptionTypes(new ArrayList<>()).build(),
            User.builder().name("Malika Tosheva").phone("+998902222222").password(passwordEncoder.encode("malika123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).subscriptionTypes(new ArrayList<>()).build(),
            User.builder().name("Bobur Rahimov").phone("+998903333333").password(passwordEncoder.encode("bobur123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).subscriptionTypes(new ArrayList<>()).build(),
            User.builder().name("Nilufar Saidova").phone("+998904444444").password(passwordEncoder.encode("nilufar123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).subscriptionTypes(new ArrayList<>()).build(),
            User.builder().name("Jasur Abdullayev").phone("+998905555555").password(passwordEncoder.encode("jasur123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).subscriptionTypes(new ArrayList<>()).build(),
            User.builder().name("Sevara Nazarova").phone("+998906666666").password(passwordEncoder.encode("sevara123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).subscriptionTypes(new ArrayList<>()).build(),
            User.builder().name("Otabek Yusupov").phone("+998907777777").password(passwordEncoder.encode("otabek123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).subscriptionTypes(new ArrayList<>()).build(),
            User.builder().name("Madina Qodirova").phone("+998908888888").password(passwordEncoder.encode("madina123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).subscriptionTypes(new ArrayList<>()).build()
        );
        for (User user : users) {
            if (userRepository.findByPhone(user.getPhone()).isEmpty()) {
                userRepository.save(user);
            }
        }
    }

    private void assignGymsAndSubscriptionTypesToUsers() {
        List<Gym> gyms = gymRepository.findAll();
        List<User> users = userRepository.findAll();
        if (gyms.isEmpty() || users.isEmpty()) return;

        Random random = new Random();
        int gymCount = gyms.size();
        int idx = 0;

        for (User user : users) {
            boolean isAdmin = user.getRoles() != null && user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN") || r.getName().equals("ROLE_SUPERADMIN"));
            if (isAdmin) continue;

            if (user.getGyms() == null || user.getGyms().isEmpty()) {
                Gym assignedGym = gyms.get(idx % gymCount);
                user.getGyms().add(assignedGym);
            }

            if (user.getSubscriptionTypes() == null || user.getSubscriptionTypes().isEmpty()) {
                Gym userGym = user.getGyms().get(0);
                List<SubscriptionType> gymSubscriptionTypes = subscriptionTypeRepository.findByGym(userGym);
                if (!gymSubscriptionTypes.isEmpty()) {
                    SubscriptionType assignedSubscriptionType = gymSubscriptionTypes.get(random.nextInt(gymSubscriptionTypes.size()));
                    user.getSubscriptionTypes().add(assignedSubscriptionType);
                    subscriptionService.subscribeUserToSubscriptionType(user, assignedSubscriptionType, assignedSubscriptionType.getPrice().intValue(), assignedSubscriptionType.getDuration());
                }
            }
            userRepository.save(user);
            idx++;
        }
    }
}
