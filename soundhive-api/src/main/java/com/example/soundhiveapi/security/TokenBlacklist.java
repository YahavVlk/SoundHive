package com.example.soundhiveapi.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds revoked JWTs in memory.
 * In a real app you'd persist this or use short expirations.
 */
@Component
public class TokenBlacklist {
    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    /** Mark this token as revoked. */
    public void revokeToken(String token) {
        revoked.add(token);
    }

    /** Check if this token is revoked. */
    public boolean isRevoked(String token) {
        return revoked.contains(token);
    }
}
