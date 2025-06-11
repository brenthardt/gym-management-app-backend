package org.example.project1.service.jwt.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.project1.service.jwt.JwtService;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final String secret = "your-256-bit-secret-your-256-bit-secret";
    private final Key secretKey = Keys.hmacShaKeyFor(secret.getBytes());

    @Override
    public String generateToken(String phone, String role) {
        long expirationMillis = 1000 * 10;
        return Jwts.builder()
                .setSubject(phone)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(String phone, String role) {
        long refreshExpirationMillis = 24 * 60 * 60 * 1000L;
        return Jwts.builder()
                .setSubject(phone)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMillis))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public Claims getClaims(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            return jws.getBody();
        } catch (JwtException e) {
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    @Override
    public Key getSecretKey() {
        return secretKey;
    }
}