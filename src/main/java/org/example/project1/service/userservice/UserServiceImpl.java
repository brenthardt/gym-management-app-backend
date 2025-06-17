package org.example.project1.service.userservice;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.project1.dto.*;
import org.example.project1.entity.Gym;
import org.example.project1.entity.Tariff;
import org.example.project1.entity.User;
import org.example.project1.repository.GymRepository;
import org.example.project1.repository.TariffRepository;
import org.example.project1.service.jwt.impl.JwtService;
import org.example.project1.repository.RoleRepository;
import org.example.project1.repository.UserRepository;
import org.example.project1.entity.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TariffRepository tariffRepository;
    private final GymRepository gymRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public User save(User user) {
        // If this is an update (user has ID), find existing user and update
        if (user.getId() != null) {
            return userRepository.findById(user.getId())
                    .map(existingUser -> {
                        // Update fields that are not null
                        if (user.getName() != null) {
                            existingUser.setName(user.getName());
                        }
                        if (user.getPhone() != null) {
                            existingUser.setPhone(user.getPhone());
                        }
                        if (user.getTelegramChatId() != null) {
                            existingUser.setTelegramChatId(user.getTelegramChatId());
                        }
                        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
                        }
                        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                            existingUser.setRoles(user.getRoles());
                        }
                        return userRepository.save(existingUser);
                    })
                    .orElseThrow(() -> new RuntimeException("User not found for update"));
        }

        // For new users
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
            user.setRoles(List.of(userRole));
        }

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        if (user.getPurchaseDate() == null) {
            user.setPurchaseDate(new Date());
        }

        return userRepository.save(user);
    }

    @Override
    public ResponseEntity<?> findAll() {
        List<UserDto> userDtos = userRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setPassword(user.getPassword());
        dto.setPurchaseDate(user.getPurchaseDate());

        if (user.getGyms() != null) {
            dto.setGyms(user.getGyms().stream()
                    .map(gym -> {
                        GymDto gymDto = new GymDto();
                        gymDto.setId(gym.getId());
                        gymDto.setName(gym.getName());
                        gymDto.setLocation(gym.getLocation());
                        return gymDto;
                    })
                    .toList());
        }

        if (user.getTariffs() != null) {
            dto.setTariffs(user.getTariffs().stream()
                    .map(tariff -> {
                        TariffDto tariffDto = new TariffDto();
                        tariffDto.setId(tariff.getId());
                        tariffDto.setName(tariff.getName());
                        tariffDto.setPrice(tariff.getPrice());
                        tariffDto.setDuration(tariff.getDuration());
                        return tariffDto;
                    })
                    .toList());
        }

        return dto;
    }

    @Override
    @Transactional
    public User saveUserWithTariffAndGym(User user, UUID tariffId, UUID gymId) {
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new RuntimeException("Tariff not found"));
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new RuntimeException("Gym not found"));

        user.getGyms().add(gym);
        user.getTariffs().add(tariff);
        gym.getMembers().add(user);
        tariff.getUsers().add(user);

        User savedUser = userRepository.save(user);
        gymRepository.save(gym);
        tariffRepository.save(tariff);

        return savedUser;
    }

    @Override
    public ResponseEntity<?> update(UUID id, User user) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setName(user.getName());
                    existingUser.setPhone(user.getPhone());

                    if (user.getPassword() != null) {
                        existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
                    }

                    if (user.getGyms() != null) {
                        existingUser.getGyms().clear();
                        existingUser.getGyms().addAll(user.getGyms());
                    }

                    if (user.getTariffs() != null) {
                        existingUser.getTariffs().clear();
                        existingUser.getTariffs().addAll(user.getTariffs());
                    }

                    if (user.getRoles() != null) {
                        existingUser.setRoles(user.getRoles());
                    }
                    return ResponseEntity.ok(userRepository.save(existingUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    public void addGymToUser(UUID userId, UUID gymId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new RuntimeException("Gym not found"));

        user.getGyms().add(gym);
        gym.getMembers().add(user);

        userRepository.save(user);
        gymRepository.save(gym);
    }

    @Transactional
    public void addTariffToUser(UUID userId, UUID tariffId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new RuntimeException("Tariff not found"));

        user.getTariffs().add(tariff);
        tariff.getUsers().add(user);

        userRepository.save(user);
        tariffRepository.save(tariff);
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

                        if (user.getTariffs() != null) {
                            for (Tariff tariff : user.getTariffs()) {
                                Integer duration = tariff.getDuration();
                                if (duration != null && duration > 0) {
                                    tariff.setDuration(duration - 1);
                                    tariffRepository.save(tariff);
                                }
                            }
                        }

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

    @Override
    public ResponseEntity<?> findByRole(String roleName) {
        List<UserDto> userDtos = userRepository.findByRoleName(roleName).stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    @Override
    public User findByChatId(Long chatId) {
        return userRepository.findByTelegramChatId(Long.valueOf(chatId))
                .orElse(null);
    }

}
