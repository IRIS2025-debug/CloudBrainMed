package com.cloudbrainmed.common.utils;

import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
@Component
public class DoctorJwtUtil {
    private static final String SECRET = "medical-cloud-jwt-secret-2026-very-long-key-32bytes";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    private static final long EXPIRE = 1000 * 60 * 60 * 24 * 7;

    public static String createToken(String userId, String phone, Integer roleType) {
        return Jwts.builder().claim("userId", userId)
                .claim("phone", phone)
                .claim("roleType", roleType)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(KEY)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String getUserId(String token) {
        return parseToken(token).get("userId", String.class);
    }
}
