package com.quran.web.service;

import com.quran.web.dto.response.BookmarkResponse;

import java.util.List;

public interface UserBookmarkService {

    /**
     * Toggle bookmark for the given ayah.
     * @return true if bookmark was added, false if it was removed
     */
    boolean toggle(Long userId, Integer surahNumber, Integer ayahNumber, String notes);

    /**
     * Returns list of bookmarked ayah numbers for a given user + surah.
     */
    List<Integer> getBookmarkedAyahNumbers(Long userId, Integer surahNumber);

    /**
     * Get all bookmarks for the given user, ordered by newest first.
     */
    List<BookmarkResponse> getAll(Long userId);

    /**
     * Delete a single bookmark by ID. Verifies ownership before deleting.
     */
    void deleteById(Long bookmarkId, Long userId);

    /**
     * Insert or update notes on the bookmark identified by ayahId.
     * Throws ResourceNotFoundException if the bookmark does not exist.
     */
    BookmarkResponse updateNotes(Long userId, Long ayahId, String notes);

    /**
     * Delete all bookmarks for the given user.
     */
    void clearAll(Long userId);
}
