package org.example.project1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginResponse {
    private UUID id;
    private String name;
    private String phone;
    private String role;
    private String token;
    private String refreshToken;
}