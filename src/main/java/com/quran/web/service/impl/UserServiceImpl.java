package com.quran.web.service.impl;

import com.quran.web.dto.request.UserRegistrationRequest;
import com.quran.web.dto.response.UserResponse;
import com.quran.web.exception.InvalidRequestException;
import com.quran.web.model.User;
import com.quran.web.repository.UserRepository;
import com.quran.web.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidRequestException("Email is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getUsername());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void updateLastRead(Long userId, Integer surahNumber, Integer ayahNumber) {
        if (surahNumber < 1 || surahNumber > 114 || ayahNumber < 1) {
            throw new InvalidRequestException("Invalid surah or ayah number");
        }
        userRepository.updateLastRead(userId, surahNumber, ayahNumber);
        log.debug("Updated last_read for user {} → {}:{}", userId, surahNumber, ayahNumber);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
