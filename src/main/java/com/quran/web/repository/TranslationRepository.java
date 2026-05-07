package com.quran.web.repository;

import com.quran.web.model.Ayah;
import com.quran.web.model.Language;
import com.quran.web.model.Translation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

    Optional<Translation> findByAyahAndLanguage(Ayah ayah, Language language);

    /**
     * ✅ OPTIMIZED: Batch load translations dengan ayah dan language
     */
    @Query("SELECT t FROM Translation t " +
            "JOIN FETCH t.ayah " +
            "JOIN FETCH t.language " +
            "WHERE t.ayah = :ayah")
    List<Translation> findByAyah(@Param("ayah") Ayah ayah);

    List<Translation> findByLanguage(Language language);

    /**
     * ✅ OPTIMIZED: Search dengan JOIN FETCH
     */
    @Query("SELECT t FROM Translation t " +
            "JOIN FETCH t.ayah a " +
            "JOIN FETCH a.surah " +
            "JOIN FETCH t.language " +
            "WHERE LOWER(t.text) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "AND (:language IS NULL OR t.language = :language)")
    Page<Translation> searchByText(@Param("searchText") String searchText,
                                   @Param("language") Language language,
                                   Pageable pageable);

    /**
     * ✅ OPTIMIZED: Batch load translations untuk multiple ayahs
     * Gunakan IN clause untuk avoid N+1
     */
    @Query("SELECT t FROM Translation t " +
            "JOIN FETCH t.language " +
            "WHERE t.ayah IN :ayahs AND t.language = :language")
    List<Translation> findByAyahInAndLanguage(
            @Param("ayahs") List<Ayah> ayahs,
            @Param("language") Language language
    );

    /**
     * ✅ OPTIMIZED: Single query untuk get translation by IDs
     */
    @Query("SELECT t FROM Translation t " +
            "JOIN FETCH t.ayah a " +
            "JOIN FETCH a.surah " +
            "JOIN FETCH t.language l " +
            "WHERE a.id = :ayahId AND l.code = :languageCode")
    Optional<Translation> findByAyahIdAndLanguageCode(
            @Param("ayahId") Long ayahId,
            @Param("languageCode") String languageCode
    );

    /**
     * ✅ SUPER OPTIMIZED: Batch load semua translations untuk surah
     * Single query dengan JOIN FETCH untuk avoid N+1
     */
    @Query("SELECT t FROM Translation t " +
            "JOIN FETCH t.ayah a " +
            "JOIN FETCH t.language l " +
            "WHERE a.surah.id = :surahId " +
            "AND l.code = :languageCode " +
            "ORDER BY a.ayahNumber")
    List<Translation> findBySurahIdAndLanguageCode(
            @Param("surahId") Long surahId,
            @Param("languageCode") String languageCode
    );

    /**
     * ✅ BATCH LOAD: Get translations untuk multiple surah sekaligus
     * Useful untuk dashboard/homepage
     */
    @Query("SELECT t FROM Translation t " +
            "JOIN FETCH t.ayah a " +
            "JOIN FETCH a.surah s " +
            "JOIN FETCH t.language l " +
            "WHERE s.id IN :surahIds " +
            "AND l.code = :languageCode " +
            "ORDER BY s.surahNumber, a.ayahNumber")
    List<Translation> findBySurahIdsAndLanguageCode(
            @Param("surahIds") List<Long> surahIds,
            @Param("languageCode") String languageCode
    );
}
