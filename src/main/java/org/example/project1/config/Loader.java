package org.example.project1.config;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.*;
import org.example.project1.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class Loader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GymRepository gymRepository;
    private final TariffRepository tariffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initGyms();
        initTariffs();
        initAdminUsers();
        initRegularUsers();
        assignGymsToUsers();
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
        List<Gym> gyms = List.of(
            Gym.builder().name("PowerGym Tashkent").location("Tashkent, Yunusobod tumani").members(new ArrayList<>()).build(),
            Gym.builder().name("FitnessPro Samarkand").location("Samarkand, Registon ko'chasi").members(new ArrayList<>()).build(),
            Gym.builder().name("BodyBuilder Gym").location("Tashkent, Chilonzor tumani").members(new ArrayList<>()).build(),
            Gym.builder().name("Iron Paradise").location("Tashkent, Mirzo Ulug'bek tumani").members(new ArrayList<>()).build(),
            Gym.builder().name("Fitness Zone").location("Namangan, Markaziy ko'cha").members(new ArrayList<>()).build()
        );
        for (Gym gym : gyms) {
            if (gymRepository.findByName(gym.getName()).isEmpty()) {
                gymRepository.save(gym);
            }
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
        final String superAdminPhone = "+998907110700";
        List<User> superAdmins = userRepository.findAll().stream()
            .filter(u -> superAdminPhone.equals(u.getPhone()))
            .toList();
        if (superAdmins.size() > 1) {
            for (int i = 1; i < superAdmins.size(); i++) {
                userRepository.delete(superAdmins.get(i));
            }
        }

        final String adminPhone = "+998907110709";
        Role superAdminRole = roleRepository.findByName("ROLE_SUPERADMIN")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_SUPERADMIN").build()));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

        if (userRepository.findByPhone(superAdminPhone).isEmpty()) {
            userRepository.save(User.builder()
                .name("SuperAdmin User")
                .phone(superAdminPhone)
                .password(passwordEncoder.encode("superadmin123"))
                .roles(List.of(superAdminRole))
                .purchaseDate(new Date())
                .gyms(new ArrayList<>())
                .tariffs(new ArrayList<>())
                .build());
        }

        if (userRepository.findByPhone(adminPhone).isEmpty()) {
            userRepository.save(User.builder()
                .name("Shaxzod")
                .phone(adminPhone)
                .password(passwordEncoder.encode("admin123"))
                .roles(List.of(adminRole))
                .purchaseDate(new Date())
                .gyms(new ArrayList<>())
                .tariffs(new ArrayList<>())
                .build());
        }
    }

    private void initRegularUsers() {
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        List<User> users = List.of(
            User.builder().name("Alisher Karimov").phone("+998901111111").password(passwordEncoder.encode("alisher123")).roles(List.of(userRole)).purchaseDate(new Date()).gyms(new ArrayList<>()).tariffs(new ArrayList<>()).build(),
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

    private void assignGymsToUsers() {
        List<Gym> gyms = gymRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Tariff> tariffs = tariffRepository.findAll();
        if (gyms.isEmpty() || users.isEmpty() || tariffs.isEmpty()) return;
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_USER"))) {
                Gym gym = gyms.get(i % gyms.size());
                Tariff tariff = tariffs.get(i % tariffs.size());
                user.getGyms().add(gym);
                user.getTariffs().add(tariff);
                gym.getMembers().add(user);
                tariff.getUsers().add(user);
                userRepository.save(user);
                gymRepository.save(gym);
                tariffRepository.save(tariff);
            }
        }
    }
}
