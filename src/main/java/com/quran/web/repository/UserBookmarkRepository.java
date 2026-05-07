package com.quran.web.repository;

import com.quran.web.model.UserBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookmarkRepository extends JpaRepository<UserBookmark, Long> {

    Optional<UserBookmark> findByUserIdAndAyahId(Long userId, Long ayahId);

    boolean existsByUserIdAndAyahId(Long userId, Long ayahId);

    void deleteByUserIdAndAyahId(Long userId, Long ayahId);

    void deleteAllByUserId(Long userId);

    // Fetch all bookmarks with ayah + surah in one query (no N+1)
    @Query("SELECT ub FROM UserBookmark ub " +
           "JOIN FETCH ub.ayah a JOIN FETCH a.surah " +
           "WHERE ub.user.id = :userId ORDER BY ub.createdAt DESC")
    List<UserBookmark> findAllByUserIdWithDetails(@Param("userId") Long userId);

    // Used before delete to verify ownership
    Optional<UserBookmark> findByIdAndUserId(Long id, Long userId);

    // Returns bookmarked ayah numbers for a specific surah — used by JS on page load
    @Query("SELECT ub.ayah.ayahNumber FROM UserBookmark ub " +
           "WHERE ub.user.id = :userId AND ub.ayah.surah.surahNumber = :surahNumber")
    List<Integer> findBookmarkedAyahNumbersByUserAndSurah(
            @Param("userId") Long userId,
            @Param("surahNumber") Integer surahNumber);
}
