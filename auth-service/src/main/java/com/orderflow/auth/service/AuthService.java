package com.orderflow.auth.service;

import com.orderflow.auth.dto.AuthResponse;
import com.orderflow.auth.dto.LoginRequest;
import com.orderflow.auth.dto.RegisterRequest;
import com.orderflow.auth.entity.User;
import com.orderflow.auth.exception.AuthenticationFailedException;
import com.orderflow.auth.exception.DuplicateResourceException;
import com.orderflow.auth.repository.UserRepository;
import com.orderflow.auth.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new DuplicateResourceException("USERNAME_TAKEN", "Username already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("EMAIL_REGISTERED", "Email already registered");
        }
        User user = new User(req.getUsername(), passwordEncoder.encode(req.getPassword()), req.getEmail());
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getId());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("Invalid username or password");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getId());
    }
}
