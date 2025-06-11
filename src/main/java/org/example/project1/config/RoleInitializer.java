package org.example.project1.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.project1.repository.RoleRepository;
import org.example.project1.role.Role;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void initRoles() {
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_SUPERADMIN");

        for (String roleName : roles) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(roleName);
                        return roleRepository.save(role);
                    });
        }

        System.out.println(" Roles: ROLE_ADMIN, ROLE_USER, ROLE_SUPERADMIN");
    }
}

