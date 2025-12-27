// com/rsvp/service/AuthService.java
package com.rsvp.service;

import com.rsvp.dto.AuthResponse;
import com.rsvp.dto.LoginRequest;
import com.rsvp.dto.RegisterRequest;
import com.rsvp.entity.User;
import com.rsvp.exception.ValidationException;
import com.rsvp.repository.UserRepository;
import com.rsvp.config.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());  // ADD THIS LINE
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        user = userRepository.save(user);

        String token = jwtTokenUtil.generateToken(user.getUsername(), user.getRole().name());
        return AuthResponse.fromUser(user, token);
    }
    // In AuthService.java - line 55
    @Transactional(readOnly = true)  // Add readOnly for queries
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ValidationException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ValidationException("Invalid username or password");
        }


        String token = jwtTokenUtil.generateToken(user.getUsername(), user.getRole().name());

        // 4) Build flat DTO for frontend
        return AuthResponse.fromUser(user, token);
    }
}
