package org.example.project1.config;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.Role;
import org.example.project1.entity.User;
import org.example.project1.repository.RoleRepository;
import org.example.project1.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Loader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initRoles();
        
        initAdminUsers();
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

        System.out.println("Roles initialized: ROLE_ADMIN, ROLE_USER, ROLE_SUPERADMIN");
    }
    
    private void initAdminUsers() {
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