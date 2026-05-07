package com.pragun.ElectiSelect.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    // In production, move this to application.properties
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    private final int jwtExpirationMs = 86400000; // 24 hours




    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("❌ JWT Token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("❌ JWT Token is unsupported: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("❌ JWT Token is malformed: " + e.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException e) {
            System.out.println("❌ JWT Signature validation failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }

    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }
}