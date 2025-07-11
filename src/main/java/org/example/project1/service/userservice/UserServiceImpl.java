package org.example.project1.service.userservice;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.project1.dto.LoginDto;
import org.example.project1.dto.LoginResponse;
import org.example.project1.dto.UserDto;
import org.example.project1.dto.GymDto;
import org.example.project1.entity.Role;
import org.example.project1.entity.User;
import org.example.project1.entity.Gym;
import org.example.project1.repository.RoleRepository;
import org.example.project1.repository.UserRepository;
import org.example.project1.repository.GymRepository;
import org.example.project1.service.jwt.impl.JwtServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtServiceImpl jwtService;

    @Override
    public User save(User user) {
        if (user.getPhone() != null) {
            List<User> existingUsers = userRepository.findAllByPhone(user.getPhone());
            if (!existingUsers.isEmpty() && (user.getId() == null || !existingUsers.stream().anyMatch(u -> u.getId().equals(user.getId())))) {
                if (existingUsers.size() > 1) {
                    System.out.println("Found duplicate phones for: " + user.getPhone() + ". Cleaning up...");
                    userRepository.deleteDuplicateUsers();
                }
                existingUsers = userRepository.findAllByPhone(user.getPhone());
                if (!existingUsers.isEmpty() && (user.getId() == null || !existingUsers.stream().anyMatch(u -> u.getId().equals(user.getId())))) {
                    throw new RuntimeException("Phone number already exists: " + user.getPhone());
                }
            }
        }
        
        if (user.getId() != null) {
            return userRepository.findById(user.getId())
                    .map(existingUser -> {
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

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
            user.setRoles(List.of(userRole));
        }

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
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

        return dto;
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
        if (userRepository.findFirstByPhone(userDto.getPhone()).isPresent()) {
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
        return userRepository.findFirstByPhone(loginDTO.getPhone())
                .map(user -> {
                    if (passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
                        String mainRole = user.getRoles().stream()
                                .findFirst()
                                .map(Role::getName)
                                .orElse("UNKNOWN");
                        String token = jwtService.generateToken(user.getPhone(), mainRole);
                        String refreshToken = jwtService.generateRefreshToken(user.getPhone(), mainRole);
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
            User user = userRepository.findFirstByPhone(phone).orElseThrow(() -> new RuntimeException("User not found"));
            String newAccessToken = jwtService.generateToken(phone, role);
            String newRefreshToken = jwtService.generateRefreshToken(phone, role);
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
        return userRepository.findByTelegramChatId((chatId))
                .orElse(null);
    }

}
