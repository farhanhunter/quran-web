package com.quran.web.service.impl;

import com.quran.web.dto.request.AyahQueryRequest;
import com.quran.web.dto.request.SearchRequest;
import com.quran.web.dto.response.*;
import com.quran.web.exception.InvalidRequestException;
import com.quran.web.exception.ResourceNotFoundException;
import com.quran.web.model.Ayah;
import com.quran.web.model.Language;
import com.quran.web.model.Surah;
import com.quran.web.model.Translation;
import com.quran.web.repository.AyahRepository;
import com.quran.web.repository.LanguageRepository;
import com.quran.web.repository.SurahRepository;
import com.quran.web.repository.TranslationRepository;
import com.quran.web.service.QuranService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation QuranService
 * SRP: Hanya handle business logic terkait Quran
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuranServiceImpl implements QuranService {

    private final SurahRepository surahRepository;
    private final AyahRepository ayahRepository;
    private final TranslationRepository translationRepository;
    private final LanguageRepository languageRepository;
    private final QuranMapper quranMapper;

    @Override
    public List<SurahResponse> getAllSurahs() {
        log.info("Fetching all surahs");

        // ✅ Single query, no N+1
        List<Surah> surahs = surahRepository.findAllByOrderBySurahNumberAsc();

        return surahs.stream()
                .map(quranMapper::toSurahResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SurahResponse> searchSurahsByName(String name) {
        return surahRepository.findByNameContaining(name).stream()
                .map(quranMapper::toSurahResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AyahDetailResponse> getSajdaAyahs() {
        return ayahRepository.findBySajdaTrueOrderBySurahIdAscAyahNumberAsc().stream()
                .map(a -> quranMapper.toAyahDetailResponse(a, null))
                .collect(Collectors.toList());
    }

    @Override
    public SurahDetailResponse getSurahDetail(Integer surahNumber, String languageCode) {
        log.info("Fetching surah detail: {}, language: {}", surahNumber, languageCode);

        // Validate surah number
        if (surahNumber < 1 || surahNumber > 114) {
            throw new InvalidRequestException("Surah number must be between 1 and 114");
        }

        // Find surah
        Surah surah = surahRepository.findBySurahNumber(surahNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Surah not found with number: " + surahNumber
                ));

        // Get language code (default to Indonesian if not specified)
        String langCode = languageCode != null ? languageCode : "id";

        // Validate language exists (optional validation)
        if (!languageRepository.existsByCode(langCode)) {
            throw new ResourceNotFoundException("Language not found: " + langCode);
        }

        // ✅ OPTIMIZED: Single query dengan JOIN FETCH untuk ayahs + surah
        List<Ayah> ayahs = ayahRepository.findBySurahOrderByAyahNumberAsc(surah);

        // ✅ OPTIMIZED: Single query untuk semua translations
        // Batch load translations instead of N queries
        List<Translation> translations = translationRepository
                .findBySurahIdAndLanguageCode(surah.getId(), langCode);

        // Map to response
        return quranMapper.toSurahDetailResponse(surah, ayahs, translations, langCode);
    }

    @Override
    public AyahDetailResponse getAyahDetail(AyahQueryRequest request) {
        log.info("Fetching ayah detail: {}:{}",
                request.getSurahNumber(), request.getAyahNumber());

        // Validate
        validateAyahQueryRequest(request);

        // ✅ OPTIMIZED: Single query dengan JOIN FETCH surah
        Ayah ayah = ayahRepository
                .findBySurahNumberAndAyahNumberWithSurah(
                        request.getSurahNumber(),
                        request.getAyahNumber()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Ayah not found: %d:%d",
                                request.getSurahNumber(),
                                request.getAyahNumber())
                ));

        // Get language
        String langCode = request.getLanguageCode() != null ?
                request.getLanguageCode() : "id";
        Language language = languageRepository.findByCode(langCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Language not found: " + langCode
                ));

        // ✅ OPTIMIZED: Single query untuk translation
        Translation translation = translationRepository
                .findByAyahAndLanguage(ayah, language)
                .orElse(null);

        // ✅ Total queries: 3 (ayah+surah, language, translation)

        // Map to response
        return quranMapper.toAyahDetailResponse(ayah, translation);
    }

    @Override
    public PageResponse<AyahResponse> searchAyahs(SearchRequest request) {
        log.info("Searching ayahs with query: {}", request.getQuery());

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize()
        );

        Page<Ayah> ayahPage;

        // Search based on search type
        switch (request.getSearchType()) {
            case TEXT:
                // ✅ OPTIMIZED: JOIN FETCH surah di query
                ayahPage = ayahRepository.searchByArabicText(
                        request.getQuery(),
                        pageable
                );
                break;

            case TRANSLATION:
                Language language = languageRepository
                        .findByCode(request.getLanguageCode())
                        .orElse(null);

                // ✅ OPTIMIZED: JOIN FETCH ayah + surah + language
                Page<Translation> translationPage = translationRepository
                        .searchByText(request.getQuery(), language, pageable);

                List<AyahResponse> translationResponses = translationPage.getContent()
                        .stream()
                        .map(t -> quranMapper.toAyahResponse(t.getAyah(), t))
                        .collect(Collectors.toList());

                return buildPageResponse(translationPage, translationResponses);

            default:
                throw new InvalidRequestException("Invalid search type");
        }

        // Map to response
        List<AyahResponse> content = ayahPage.getContent()
                .stream()
                .map(quranMapper::toAyahResponse)
                .collect(Collectors.toList());

        return PageResponse.<AyahResponse>builder()
                .content(content)
                .currentPage(ayahPage.getNumber())
                .totalPages(ayahPage.getTotalPages())
                .totalElements(ayahPage.getTotalElements())
                .size(ayahPage.getSize())
                .hasNext(ayahPage.hasNext())
                .hasPrevious(ayahPage.hasPrevious())
                .build();
    }

    // Helper method
    private void validateAyahQueryRequest(AyahQueryRequest request) {
        if (request.getSurahNumber() < 1 || request.getSurahNumber() > 114) {
            throw new InvalidRequestException(
                    "Surah number must be between 1 and 114"
            );
        }

        // Validate ayah number exists in surah
        Surah surah = surahRepository.findBySurahNumber(request.getSurahNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Surah not found: " + request.getSurahNumber()
                ));

        if (request.getAyahNumber() < 1 ||
                request.getAyahNumber() > surah.getTotalAyahs()) {
            throw new InvalidRequestException(
                    String.format("Invalid ayah number. Surah %d has %d ayahs",
                            request.getSurahNumber(), surah.getTotalAyahs())
            );
        }
    }

    private <T> PageResponse<T> buildPageResponse(Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .size(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
