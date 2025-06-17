package org.example.project1.config;

import lombok.RequiredArgsConstructor;
import org.example.project1.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.example.project1.entity.Role;
import org.example.project1.entity.User;
import org.example.project1.entity.Gym;
import org.example.project1.entity.Tariff;

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
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_SUPERADMIN");
        for (String roleName : roles) {
            roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleName);
                    return roleRepository.save(role);
                });
        }
    }

    private void initGyms() {
        List<String[]> gymData = List.of(
            new String[]{"PowerGym Tashkent", "Tashkent, Yunusobod tumani"},
            new String[]{"FitnessPro Samarkand", "Samarkand, Registon ko'chasi"},
            new String[]{"BodyBuilder Gym", "Tashkent, Chilonzor tumani"},
            new String[]{"Iron Paradise", "Tashkent, Mirzo Ulug'bek tumani"},
            new String[]{"Fitness Zone", "Namangan, Markaziy ko'cha"}
        );
        for (String[] data : gymData) {
            String name = data[0];
            String location = data[1];
            if (gymRepository.findByName(name).isEmpty()) {
                Gym gym = new Gym();
                gym.setName(name);
                gym.setLocation(location);
                gym.setMembers(new ArrayList<>());
                gymRepository.save(gym);
            }
        }
    }

    private void initTariffs() {
        List<Object[]> tariffData = List.of(
            new Object[]{"Basic Plan", 150000.0, 30, "Asosiy mashg'ulotlar"},
            new Object[]{"Premium Plan", 250000.0, 30, "Barcha mashg'ulotlar + trener"},
            new Object[]{"VIP Plan", 400000.0, 30, "Individual trener + spa"},
            new Object[]{"Student Plan", 100000.0, 30, "Talabalar uchun chegirma"},
            new Object[]{"Family Plan", 500000.0, 30, "Oila uchun (4 kishi)"},
            new Object[]{"Annual Basic", 1500000.0, 365, "Yillik basic plan"},
            new Object[]{"Annual Premium", 2500000.0, 365, "Yillik premium plan"}
        );
        for (Object[] data : tariffData) {
            String name = (String) data[0];
            Double price = (Double) data[1];
            Integer duration = (Integer) data[2];
            String description = (String) data[3];
            if (tariffRepository.findByName(name).isEmpty()) {
                Tariff tariff = new Tariff();
                tariff.setName(name);
                tariff.setPrice(price);
                tariff.setDuration(duration);
                tariff.setDescription(description);
                tariff.setUsers(new ArrayList<>());
                tariffRepository.save(tariff);
            }
        }
    }

    private void initAdminUsers() {
        Role superAdminRole = roleRepository.findByName("ROLE_SUPERADMIN")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_SUPERADMIN")));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));
        String superAdminPhone = "+998901234567";
        if (userRepository.findByPhone(superAdminPhone).isEmpty()) {
            User superAdmin = new User();
            superAdmin.setName("SuperAdmin User");
            superAdmin.setPhone(superAdminPhone);
            superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setRoles(List.of(superAdminRole));
            superAdmin.setPurchaseDate(new Date());
            superAdmin.setGyms(new ArrayList<>());
            superAdmin.setTariffs(new ArrayList<>());
            userRepository.save(superAdmin);
        }
        String adminPhone = "+998907110709";
        if (userRepository.findByPhone(adminPhone).isEmpty()) {
            User admin = new User();
            admin.setName("Admin User");
            admin.setPhone(adminPhone);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(List.of(adminRole));
            admin.setPurchaseDate(new Date());
            admin.setGyms(new ArrayList<>());
            admin.setTariffs(new ArrayList<>());
            userRepository.save(admin);
        }
    }

    private void initRegularUsers() {
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));
        List<String[]> userData = List.of(
            new String[]{"Alisher Karimov", "+998901111111", "alisher123"},
            new String[]{"Malika Tosheva", "+998902222222", "malika123"},
            new String[]{"Bobur Rahimov", "+998903333333", "bobur123"},
            new String[]{"Nilufar Saidova", "+998904444444", "nilufar123"},
            new String[]{"Jasur Abdullayev", "+998905555555", "jasur123"},
            new String[]{"Sevara Nazarova", "+998906666666", "sevara123"},
            new String[]{"Otabek Yusupov", "+998907777777", "otabek123"},
            new String[]{"Madina Qodirova", "+998908888888", "madina123"}
        );
        for (String[] data : userData) {
            String name = data[0];
            String phone = data[1];
            String password = data[2];
            if (userRepository.findByPhone(phone).isEmpty()) {
                User user = new User();
                user.setName(name);
                user.setPhone(phone);
                user.setPassword(passwordEncoder.encode(password));
                user.setRoles(List.of(userRole));
                user.setPurchaseDate(new Date());
                user.setGyms(new ArrayList<>());
                user.setTariffs(new ArrayList<>());
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
