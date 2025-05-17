package com.example.soundhiveapi.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds revoked JWTs in memory.
 * This is used to block tokens after logout or abuse.
 * In a production system, consider persisting this in a cache or database.
 */
@Component
public class TokenBlacklist {

    // Thread-safe set to store revoked JWT strings
    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    /**
     * Mark the given token as revoked (e.g., on logout).
     * @param token JWT string to blacklist
     */
    public void revokeToken(String token) {
        revoked.add(token);
    }

    /**
     * Check if a token is currently revoked.
     * Used during authentication filter processing.
     * @param token JWT string to check
     * @return true if token has been blacklisted
     */
    public boolean isRevoked(String token) {
        return revoked.contains(token);
    }
}
