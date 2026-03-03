package com.ecads.pulse.authentication.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String JWT_SECRET = "UpUjPWX5USVxl4g7fOIGZ6sAbCdXli2TUMGkHA6jloQ=";
    private static final Key HMAC_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    private static final long TOKEN_EXPIRATION = 5 * 60 * 1000;
    private static final long REFRESH_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000;

    public static String generateToken(String firebaseToken) {
        return Jwts.builder()
                .subject(firebaseToken)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION))
                .signWith(HMAC_KEY)
                .compact();
    }

    public static String generateRefreshToken(String firebaseToken) {
        return Jwts.builder()
                .subject(firebaseToken)
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(HMAC_KEY)
                .compact();
    }

    public static String validateToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
