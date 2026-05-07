package com.quran.web.controller;

import com.quran.web.dto.request.ReadingProgressRequest;
import com.quran.web.dto.response.ApiResponse;
import com.quran.web.security.CustomUserDetails;
import com.quran.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReadingProgressController {

    private final UserService userService;

    @PostMapping("/reading-progress")
    public ResponseEntity<ApiResponse<Void>> saveProgress(
            @Valid @RequestBody ReadingProgressRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        userService.updateLastRead(
                userDetails.getUserId(),
                request.getSurahNumber(),
                request.getAyahNumber()
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
