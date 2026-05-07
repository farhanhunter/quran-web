package com.quran.web.repository;

import com.quran.web.model.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {

    Optional<Language> findByCode(String code);

    List<Language> findByIsActiveTrue();

    boolean existsByCode(String code);

    @Query("SELECT l FROM Language l WHERE LOWER(l.code) = LOWER(:code)")
    Optional<Language> findByCodeIgnoreCase(@Param("code") String code);
}
