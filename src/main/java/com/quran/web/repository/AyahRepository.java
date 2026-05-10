package com.quran.web.repository;

import com.quran.web.model.Ayah;
import com.quran.web.model.Surah;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AyahRepository extends JpaRepository<Ayah, Long> {

    /**
     * ✅ OPTIMIZED: Eager load surah dengan JOIN FETCH
     * Menghindari N+1 query saat akses surah.nameLatin
     */
    @Query("SELECT a FROM Ayah a " +
            "JOIN FETCH a.surah " +
            "WHERE a.surah = :surah " +
            "ORDER BY a.ayahNumber")
    List<Ayah> findBySurahOrderByAyahNumberAsc(@Param("surah") Surah surah);

    /**
     * ✅ OPTIMIZED: Single query dengan JOIN FETCH
     */
    @Query("SELECT a FROM Ayah a " +
            "LEFT JOIN FETCH a.surah " +
            "WHERE a.surah.surahNumber = :surahNumber " +
            "AND a.ayahNumber = :ayahNumber")
    Optional<Ayah> findBySurahNumberAndAyahNumberWithSurah(
            @Param("surahNumber") Integer surahNumber,
            @Param("ayahNumber") Integer ayahNumber
    );

    /**
     * ✅ OPTIMIZED: Batch load ayahs by juz dengan eager fetch surah
     */
    @Query("SELECT a FROM Ayah a " +
            "JOIN FETCH a.surah " +
            "WHERE a.juzNumber = :juzNumber " +
            "ORDER BY a.surah.surahNumber, a.ayahNumber")
    List<Ayah> findByJuzNumberOrderBySurahIdAscAyahNumberAsc(
            @Param("juzNumber") Integer juzNumber
    );

    /**
     * ✅ OPTIMIZED: Batch load ayahs by page
     */
    @Query("SELECT a FROM Ayah a " +
            "JOIN FETCH a.surah " +
            "WHERE a.pageNumber = :pageNumber " +
            "ORDER BY a.surah.surahNumber, a.ayahNumber")
    List<Ayah> findByPageNumberOrderBySurahIdAscAyahNumberAsc(
            @Param("pageNumber") Integer pageNumber
    );

    /**
     * ✅ OPTIMIZED: Get all sajda ayahs with surah info
     */
    @Query("SELECT a FROM Ayah a " +
            "JOIN FETCH a.surah " +
            "WHERE a.sajda = true " +
            "ORDER BY a.surah.surahNumber, a.ayahNumber")
    List<Ayah> findBySajdaTrueOrderBySurahIdAscAyahNumberAsc();

    /**
     * ✅ OPTIMIZED: Search dengan pagination
     */
    @Query("SELECT a FROM Ayah a " +
            "JOIN FETCH a.surah " +
            "WHERE a.textArabic LIKE %:searchText% " +
            "OR a.textSimple LIKE %:searchText%")
    Page<Ayah> searchByArabicText(@Param("searchText") String searchText,
                                  Pageable pageable);

}
