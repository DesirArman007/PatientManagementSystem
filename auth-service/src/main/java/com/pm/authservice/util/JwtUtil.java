package com.pm.authservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret){
        byte[] keyBytes = Base64.getDecoder()
                .decode(secret.getBytes(StandardCharsets.UTF_8));

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email,String role){
        return Jwts.builder()
                .subject(email) //used to store an id that relates toa person who login
                .claim("role",role) // its a custom property that wer can add to jwt
                .issuedAt(new Date()) // used to determine if the token is valid or not
                .expiration(new Date(System.currentTimeMillis()+1000*60*60*10)) // 10 hours
                .signWith(secretKey) // means encode this token with secret key
                .compact(); // squash everything into a single string
    }

}
