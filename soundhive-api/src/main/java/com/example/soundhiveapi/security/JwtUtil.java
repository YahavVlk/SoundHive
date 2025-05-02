package com.example.soundhiveapi.security;

import io.jsonwebtoken.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET = "replace_with_a_strong_secret_key";
    private static final long   EXP_MS = 60 * 60 * 1000; // 1 hour

    /** Generate a JWT for the given user. */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXP_MS))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    /** Extract the username (email) from a token. */
    public String extractUsername(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /** Validate token’s signature and expiration. */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject().equals(userDetails.getUsername()) &&
                    claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
