package org.example.project1.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.project1.entity.User;
import org.example.project1.repository.RoleRepository;
import org.example.project1.repository.UserRepository;
import org.example.project1.role.Role;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initAdminUser() {
        Role superAdminRole = roleRepository.findByName("ROLE_SUPERADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_SUPERADMIN")));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));

        String superAdminPhone = "1056";
        if (userRepository.findByPhone(superAdminPhone).isEmpty()) {
            User superAdmin = new User();
            superAdmin.setName("SuperAdmin");
            superAdmin.setPhone(superAdminPhone);
            superAdmin.setPassword(passwordEncoder.encode("1056"));
            superAdmin.setRoles(List.of(superAdminRole));

            userRepository.save(superAdmin);
            System.out.println("SuperAdmin created with phone '1056' and password '1056'");
        }

        String adminPhone = "1057";
        if (userRepository.findByPhone(adminPhone).isEmpty()) {
            User admin = new User();
            admin.setName("Admin");
            admin.setPhone(adminPhone);
            admin.setPassword(passwordEncoder.encode("1057"));
            admin.setRoles(List.of(adminRole));

            userRepository.save(admin);
            System.out.println("Admin created with phone '1057' and password '1057'");
        }
    }
}

