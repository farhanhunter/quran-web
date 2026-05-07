package com.quran.web.controller;

import com.quran.web.dto.request.BookmarkRequest;
import com.quran.web.dto.request.ToggleBookmarkRequest;
import com.quran.web.dto.response.BookmarkResponse;
import com.quran.web.dto.response.ApiResponse;
import com.quran.web.security.CustomUserDetails;
import com.quran.web.service.UserBookmarkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookmarkController {

    private final UserBookmarkService userBookmarkService;

    @PostMapping("/bookmark/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggle(
            @Valid @RequestBody ToggleBookmarkRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean bookmarked = userBookmarkService.toggle(
                userDetails.getUserId(),
                request.getSurahNumber(),
                request.getAyahNumber(),
                request.getNotes()
        );

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "bookmarked", bookmarked,
                "ayahNumber", request.getAyahNumber()
        )));
    }

    @GetMapping("/bookmarks/{surahNumber}")
    public ResponseEntity<ApiResponse<List<Integer>>> getBookmarks(
            @PathVariable @Min(1) @Max(114) Integer surahNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<Integer> ayahNumbers = userBookmarkService
                .getBookmarkedAyahNumbers(userDetails.getUserId(), surahNumber);

        return ResponseEntity.ok(ApiResponse.success(ayahNumbers));
    }

    @PutMapping("/bookmark/notes")
    public ResponseEntity<ApiResponse<BookmarkResponse>> updateNotes(
            @Valid @RequestBody BookmarkRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        BookmarkResponse response = userBookmarkService.updateNotes(
                userDetails.getUserId(),
                request.getAyahId(),
                request.getNotes()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/bookmark/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOne(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        userBookmarkService.deleteById(id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/bookmarks")
    public ResponseEntity<ApiResponse<Void>> clearAll(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        userBookmarkService.clearAll(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
