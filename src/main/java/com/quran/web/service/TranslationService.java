package com.quran.web.service;

import com.quran.web.dto.response.LanguageResponse;
import com.quran.web.dto.response.TranslationResponse;

import java.util.List;

public interface TranslationService {
    List<LanguageResponse> getAvailableLanguages();
    TranslationResponse getTranslation(Long ayahId, String languageCode);
    List<TranslationResponse> getTranslations(Long ayahId);
}
