package com.example.movietickets.service;

import com.example.movietickets.dto.request.RegisterRequest;
import com.example.movietickets.entity.Role;
import com.example.movietickets.entity.User;
import com.example.movietickets.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        // role is always CUSTOMER here — self-registration can never create an admin
        User user = new User(null, request.name(), request.email(),
            passwordEncoder.encode(request.password()), Role.CUSTOMER);
        return userRepository.save(user);
    }
}
