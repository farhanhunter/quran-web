package com.quran.web.service.impl;

import com.quran.web.dto.response.LanguageResponse;
import com.quran.web.dto.response.TranslationResponse;
import com.quran.web.exception.ResourceNotFoundException;
import com.quran.web.model.Ayah;
import com.quran.web.model.Language;
import com.quran.web.model.Translation;
import com.quran.web.repository.AyahRepository;
import com.quran.web.repository.LanguageRepository;
import com.quran.web.repository.TranslationRepository;
import com.quran.web.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TranslationServiceImpl implements TranslationService {

    private final LanguageRepository languageRepository;
    private final TranslationRepository translationRepository;
    private final AyahRepository ayahRepository;
    private final QuranMapper quranMapper;

    @Override
    public List<LanguageResponse> getAvailableLanguages() {
        log.info("Fetching available languages");

        List<Language> languages = languageRepository.findByIsActiveTrue();

        return languages.stream()
                .map(quranMapper::toLanguageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TranslationResponse getTranslation(Long ayahId, String languageCode) {
        log.info("Fetching translation for ayah: {}, language: {}",
                ayahId, languageCode);

        Ayah ayah = ayahRepository.findById(ayahId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ayah not found: " + ayahId
                ));

        Language language = languageRepository.findByCode(languageCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Language not found: " + languageCode
                ));

        Translation translation = translationRepository
                .findByAyahAndLanguage(ayah, language)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Translation not found for ayah %d in %s",
                                ayahId, languageCode)
                ));

        return quranMapper.toTranslationResponse(translation);
    }

    @Override
    public List<TranslationResponse> getTranslations(Long ayahId) {
        log.info("Fetching all translations for ayah: {}", ayahId);

        Ayah ayah = ayahRepository.findById(ayahId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ayah not found: " + ayahId
                ));

        List<Translation> translations = translationRepository.findByAyah(ayah);

        return translations.stream()
                .map(quranMapper::toTranslationResponse)
                .collect(Collectors.toList());
    }
}
