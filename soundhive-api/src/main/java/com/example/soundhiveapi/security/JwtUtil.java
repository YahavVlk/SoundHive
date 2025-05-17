package com.example.soundhiveapi.security;

import io.jsonwebtoken.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = "replace_with_a_strong_secret_key"; // Secret key for signing JWTs
    private static final long   EXP_MS = 60 * 60 * 1000; // Token expiration time: 1 hour (in milliseconds)

    /**
     * Generate a JWT for the authenticated user.
     * Token contains the user's email as the subject and an expiration time.
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userDetails.getUsername())           // Set subject = email
                .setIssuedAt(now)                                // Token issue time
                .setExpiration(new Date(now.getTime() + EXP_MS)) // Token expiry
                .signWith(SignatureAlgorithm.HS256, SECRET)      // Sign using HS256 and secret
                .compact();
    }

    /**
     * Extract the subject (user email) from the given token.
     * Used to identify the user during request filtering.
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validate the token:
     * - Checks if the signature is correct
     * - Checks if the token is not expired
     * - Compares the subject with the expected user
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject().equals(userDetails.getUsername()) &&
                    claims.getExpiration().after(new Date());

        } catch (JwtException | IllegalArgumentException e) {
            // Token is malformed, expired, or signature is invalid
            return false;
        }
    }
}
