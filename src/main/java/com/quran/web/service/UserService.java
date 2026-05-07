package com.quran.web.service;

import com.quran.web.dto.request.UserRegistrationRequest;
import com.quran.web.dto.response.UserResponse;

public interface UserService {
    UserResponse register(UserRegistrationRequest request);
    void updateLastRead(Long userId, Integer surahNumber, Integer ayahNumber);
}
