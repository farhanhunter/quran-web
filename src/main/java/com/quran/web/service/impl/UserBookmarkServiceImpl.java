package com.quran.web.service.impl;

import com.quran.web.dto.response.BookmarkResponse;
import com.quran.web.exception.ResourceNotFoundException;
import com.quran.web.model.Ayah;
import com.quran.web.model.User;
import com.quran.web.model.UserBookmark;
import com.quran.web.repository.AyahRepository;
import com.quran.web.repository.UserBookmarkRepository;
import com.quran.web.repository.UserRepository;
import com.quran.web.service.UserBookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBookmarkServiceImpl implements UserBookmarkService {

    private final UserBookmarkRepository userBookmarkRepository;
    private final UserRepository userRepository;
    private final AyahRepository ayahRepository;

    @Override
    @Transactional
    public boolean toggle(Long userId, Integer surahNumber, Integer ayahNumber, String notes) {
        Ayah ayah = ayahRepository
                .findBySurahNumberAndAyahNumberWithSurah(surahNumber, ayahNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ayah not found: " + surahNumber + ":" + ayahNumber));

        Optional<UserBookmark> existing =
                userBookmarkRepository.findByUserIdAndAyahId(userId, ayah.getId());

        if (existing.isPresent()) {
            userBookmarkRepository.deleteByUserIdAndAyahId(userId, ayah.getId());
            log.debug("Bookmark removed: user={} ayah={}:{}", userId, surahNumber, ayahNumber);
            return false;
        }

        User userRef = userRepository.getReferenceById(userId);
        UserBookmark bookmark = UserBookmark.builder()
                .user(userRef)
                .ayah(ayah)
                .notes(notes)
                .build();
        userBookmarkRepository.save(bookmark);
        log.debug("Bookmark added: user={} ayah={}:{} notes={}", userId, surahNumber, ayahNumber, notes);
        return true;
    }

    private BookmarkResponse toResponse(UserBookmark ub) {
        Ayah ayah = ub.getAyah();
        return BookmarkResponse.builder()
                .id(ub.getId())
                .ayahId(ayah.getId())
                .surahNumber(ayah.getSurah().getSurahNumber())
                .surahName(ayah.getSurah().getNameLatin())
                .ayahNumber(ayah.getAyahNumber())
                .ayahTextArabic(ayah.getTextArabic())
                .notes(ub.getNotes())
                .createdAt(ub.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getBookmarkedAyahNumbers(Long userId, Integer surahNumber) {
        return userBookmarkRepository
                .findBookmarkedAyahNumbersByUserAndSurah(userId, surahNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookmarkResponse> getAll(Long userId) {
        return userBookmarkRepository.findAllByUserIdWithDetails(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long bookmarkId, Long userId) {
        UserBookmark bookmark = userBookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found: " + bookmarkId));
        userBookmarkRepository.delete(bookmark);
        log.debug("Bookmark deleted: id={} user={}", bookmarkId, userId);
    }

    @Override
    @Transactional
    public BookmarkResponse updateNotes(Long userId, Long ayahId, String notes) {
        UserBookmark bookmark = userBookmarkRepository.findByUserIdAndAyahId(userId, ayahId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bookmark not found for ayah id: " + ayahId));
        bookmark.setNotes(notes);
        return toResponse(userBookmarkRepository.save(bookmark));
    }

    @Override
    @Transactional
    public void clearAll(Long userId) {
        userBookmarkRepository.deleteAllByUserId(userId);
        log.info("All bookmarks cleared for user={}", userId);
    }
}
