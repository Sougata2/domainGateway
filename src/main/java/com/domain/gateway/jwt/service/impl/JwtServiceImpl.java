package com.domain.gateway.jwt.service.impl;

import com.domain.gateway.jwt.properties.JwtProperties;
import com.domain.gateway.jwt.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl  implements JwtService {
    private final JwtProperties properties;

    @Override
    public String validateToken(String token) {
        try {
            return extractUsername(token);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractUsername(String token){
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(properties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
