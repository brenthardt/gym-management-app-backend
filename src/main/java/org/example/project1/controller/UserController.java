package org.example.project1.controller;

import lombok.RequiredArgsConstructor;
import org.example.project1.dto.LoginDto;
import org.example.project1.dto.UserDto;
import org.example.project1.entity.User;
import org.example.project1.service.userservice.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        return userService.findAll();
    }

    @GetMapping("/by-role")
    public ResponseEntity<?> getUsersByRole(@RequestParam String role) {
        return userService.findByRole(role);
    }

    @PostMapping
    public User save(@RequestBody User user) {
        return userService.save(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return userService.delete(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id,@RequestBody User user) {
        return userService.update(id, user);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody UserDto userDto) {
        return userService.signUp(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto LoginDto) {
        return userService.login(LoginDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> req) {
        return userService.logout(req.get("phone"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> req) {
        return userService.refreshToken(req.get("refreshToken"));
    }
}
