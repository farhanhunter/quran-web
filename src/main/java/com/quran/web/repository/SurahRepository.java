package com.quran.web.repository;

import com.quran.web.model.RevelationType;
import com.quran.web.model.Surah;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurahRepository extends JpaRepository<Surah, Long> {

    Optional<Surah> findBySurahNumber(Integer surahNumber);

    @Query("SELECT s FROM Surah s WHERE " +
            "LOWER(s.nameArabic) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(s.nameLatin) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(s.nameTranslation) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Surah> findByNameContaining(@Param("name") String name);

    List<Surah> findByRevelationType(RevelationType revelationType);

    List<Surah> findAllByOrderBySurahNumberAsc();

    boolean existsBySurahNumber(Integer surahNumber);
}
