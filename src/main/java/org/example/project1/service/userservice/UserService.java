package org.example.project1.service.userservice;


import org.example.project1.dto.LoginDto;
import org.example.project1.dto.UserDto;
import org.example.project1.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface UserService {
    ResponseEntity<?> findAll();
    User save(User user);
    User saveUserWithTariffAndGym(User user, UUID tariffId, UUID gymId);
    ResponseEntity<?> delete(UUID id);
    ResponseEntity<?> update(UUID id, User user);
    ResponseEntity<?> signUp(UserDto userDto);
    ResponseEntity<?> login(LoginDto loginDto);
    ResponseEntity<?> logout(String phone);
    ResponseEntity<?> refreshToken(String refreshToken);
    ResponseEntity<?> findByRole(String roleName);
    User findByChatId(Long chatId);


}
