package org.example.project1.service.jwt;

import io.jsonwebtoken.Claims;
import java.security.Key;

public interface JwtService {
    String generateToken(String phone, String role);
    String generateRefreshToken(String phone, String role);
    Claims getClaims(String token);
    Key getSecretKey();
}