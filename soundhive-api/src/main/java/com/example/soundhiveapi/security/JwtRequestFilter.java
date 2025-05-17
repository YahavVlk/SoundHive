package com.example.soundhiveapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JdbcUserDetailsService userDetailsService; // Loads user info for auth

    @Autowired
    private JwtUtil jwtUtil; // Token parser and validator

    @Autowired
    private TokenBlacklist tokenBlacklist; // Holds revoked tokens

    /**
     * This method runs for every HTTP request to check for a valid JWT token.
     * If present and valid, it authenticates the user into the security context.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7); // Remove "Bearer " prefix

            String email = jwtUtil.extractUsername(jwt); // Extract email from token

            // Reject request if token was explicitly revoked (e.g., logout)
            if (tokenBlacklist.isRevoked(jwt)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                return;
            }

            // Proceed only if token contains a user and no authentication exists yet
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails ud = userDetailsService.loadUserByUsername(email);
                if (jwtUtil.validateToken(jwt, ud)) {
                    // Build Spring Security authentication token and inject it
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        // Continue request filter chain
        chain.doFilter(request, response);
    }
}
