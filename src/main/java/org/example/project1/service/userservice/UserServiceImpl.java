package org.example.project1.service.userservice;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.project1.dto.*;
import org.example.project1.entity.User;
import org.example.project1.service.jwt.JwtService;
import org.example.project1.repository.RoleRepository;
import org.example.project1.repository.UserRepository;
import org.example.project1.entity.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public ResponseEntity<?> findAll() {
        List<User> users = userRepository.findAll();

        List<UserDto> userDtos = users.stream().map(user -> {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setPhone(user.getPhone());
            dto.setPassword(user.getPassword());

            if (user.getPlan() != null) {
                TariffDto planDto = new TariffDto();
                planDto.setId(user.getPlan().getId());
                planDto.setName(user.getPlan().getName());
                planDto.setPrice(user.getPlan().getPrice());
                dto.setPlan(planDto);
            }

            if (user.getGym() != null) {
                GymDto gymDto = new GymDto();
                gymDto.setId(user.getGym().getId());
                gymDto.setName(user.getGym().getName());
                gymDto.setLocation(user.getGym().getLocation());
                dto.setGym(gymDto);
            }

            return dto;
        }).toList();

        return ResponseEntity.ok(userDtos);
    }


    @Override
    public User save(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
            user.setRoles(List.of(userRole));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getPurchaseDate() == null) {
            user.setPurchaseDate(new Date());
        }

        return userRepository.save(user);
    }

    @Override
    public ResponseEntity<?> delete(UUID id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok("User deleted");
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<?> update(UUID id, User user) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setName(user.getName());
                    existingUser.setPassword(user.getPassword());
                    existingUser.setPhone(user.getPhone());
                    existingUser.setGym(user.getGym());
                    existingUser.setPlan(user.getPlan());
                    existingUser.setSubDays(user.getSubDays());
                    existingUser.setPurchaseDate(new Date());
                    existingUser.setRoles(user.getRoles());

                    return ResponseEntity.ok(userRepository.save(existingUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<?> signUp(UserDto userDto) {
        if (userRepository.existsByPhone(userDto.getPhone())) {
            return ResponseEntity.badRequest().body("User already exists with this phone");
        }

        User user = new User();
        user.setName(userDto.getName());
        user.setPhone(userDto.getPhone());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));



        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
        user.setRoles(List.of(userRole));

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getPhone(), "ROLE_USER");
        String refreshToken = jwtService.generateRefreshToken(savedUser.getPhone(), "ROLE_USER");

        savedUser.setRefreshToken(refreshToken);
        userRepository.save(savedUser);

        LoginResponse response = new LoginResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getPhone(),
                "ROLE_USER",
                token,
                refreshToken
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> login(LoginDto loginDTO) {
        return userRepository.findByPhone(loginDTO.getPhone())
                .map(user -> {
                    if (passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
                        String mainRole = user.getRoles().stream()
                                .findFirst()
                                .map(Role::getName)
                                .orElse("UNKNOWN");

                        String token = jwtService.generateToken(user.getPhone(), mainRole);
                        String refreshToken = jwtService.generateRefreshToken(user.getPhone(), mainRole);



                        user.setRefreshToken(refreshToken);
                        userRepository.save(user);

                        LoginResponse response = new LoginResponse(
                                user.getId(),
                                user.getName(),
                                user.getPhone(),
                                mainRole,
                                token,
                                refreshToken
                        );

                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body("Invalid password");
                    }
                })
                .orElse(ResponseEntity.badRequest().body("User not found"));
    }

    @Override
    public ResponseEntity<?> logout(String phone) {
        return ResponseEntity.ok("User logged out");
    }

    @Override
    public ResponseEntity<?> refreshToken(String refreshToken) {
        try {
            Claims claims = jwtService.getClaims(refreshToken);
            String phone = claims.getSubject();
            String role = claims.get("role", String.class);

            User user = userRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Invalid refresh token (not found in DB)"));

            if (!user.getPhone().equals(phone)) {
                return ResponseEntity.status(401).body("Token subject mismatch");
            }

            String newAccessToken = jwtService.generateToken(phone, role);
            String newRefreshToken = jwtService.generateRefreshToken(phone, role);
            user.setRefreshToken(newRefreshToken);
            userRepository.save(user);



            return ResponseEntity.ok(
                    new LoginResponse(
                            user.getId(),
                            user.getName(),
                            user.getPhone(),
                            role,
                            newAccessToken,
                            newRefreshToken
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();
            return ResponseEntity.status(403).body("Refresh token expired or invalid");
        }
    }


}
