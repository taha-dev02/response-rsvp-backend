package com.rsvp.config;

import com.rsvp.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        // Skip JWT authentication for public endpoints
        if (isPublicEndpoint(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username = null;

        try {
            username = jwtTokenUtil.extractUsername(jwt);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for request: {} {}", method, path);
            // Don't log full stack trace for expired tokens - it's expected behavior
            filterChain.doFilter(request, response);
            return;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                if (jwtTokenUtil.validateToken(jwt, username)) {

                    UserDetails userDetails = userRepository.findByUsername(username)
                            .orElse(null);

                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (ExpiredJwtException e) {
                log.warn("JWT token expired during validation");
            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Auth endpoints
        if (path.startsWith("/api/auth")) {
            return true;
        }

        // Actuator endpoints
        if (path.startsWith("/actuator")) {
            return true;
        }

        // Public RSVP endpoints (guest submissions without login)
        if (path.matches("/api/rsvp/.*")) {
            return true;
        }

        // Public event endpoint
        if ("GET".equals(method) && path.matches("/api/events/.*/public")) {
            return true;
        }

        // Group RSVP sync endpoint
        if ("POST".equals(method) && path.equals("/api/rsvps/group-sync")) {
            return true;
        }

        // Tracking endpoints
        if (path.startsWith("/api/tracking")) {
            return true;
        }

        // Public guest endpoints (for RSVP pages)
        if (path.startsWith("/api/guests/token/")) {
            return true;
        }

        return false;
    }
}