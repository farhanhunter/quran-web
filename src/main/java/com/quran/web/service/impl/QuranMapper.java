package com.quran.web.service.impl;

import com.quran.web.dto.response.*;
import com.quran.web.model.Ayah;
import com.quran.web.model.Language;
import com.quran.web.model.Surah;
import com.quran.web.model.Translation;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuranMapper {
    public SurahResponse toSurahResponse(Surah entity) {
        return SurahResponse.builder()
                .id(entity.getId())
                .surahNumber(entity.getSurahNumber())
                .nameArabic(entity.getNameArabic())
                .nameLatin(entity.getNameLatin())
                .nameTranslation(entity.getNameTranslation())
                .totalAyahs(entity.getTotalAyahs())
                .revelationType(entity.getRevelationType().name())
                .revelationOrder(entity.getRevelationOrder())
                .build();
    }

    private static final String BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ";

    public SurahDetailResponse toSurahDetailResponse(
            Surah surah,
            List<Ayah> ayahs,
            List<Translation> translations,
            String languageCode) {

        // Create map for quick translation lookup
        var translationMap = translations.stream()
                .collect(Collectors.toMap(
                        t -> t.getAyah().getId(),
                        Translation::getText
                ));

        int surahNumber = surah.getSurahNumber();
        // Surah 1 (Al-Fatihah): bismillah is ayah 1 itself — no separate block
        // Surah 9 (At-Tawbah): no bismillah at all
        // All others: show bismillah centered and strip it from ayah 1
        String basmala = (surahNumber == 1 || surahNumber == 9) ? null : BISMILLAH;

        List<AyahResponse> ayahResponses = ayahs.stream()
                .map(ayah -> {
                    String textArabic = ayah.getTextArabic();
                    if (basmala != null && ayah.getAyahNumber() == 1) {
                        String normText = Normalizer.normalize(textArabic, Normalizer.Form.NFC);
                        String normPrefix = Normalizer.normalize(BISMILLAH + " ", Normalizer.Form.NFC);
                        if (normText.startsWith(normPrefix)) {
                            textArabic = normText.substring(normPrefix.length());
                        }
                    }
                    return AyahResponse.builder()
                            .id(ayah.getId())
                            .ayahNumber(ayah.getAyahNumber())
                            .textArabic(textArabic)
                            .juzNumber(ayah.getJuzNumber())
                            .pageNumber(ayah.getPageNumber())
                            .sajda(ayah.getSajda())
                            .translation(translationMap.get(ayah.getId()))
                            .build();
                })
                .collect(Collectors.toList());

        return SurahDetailResponse.builder()
                .id(surah.getId())
                .surahNumber(surah.getSurahNumber())
                .nameArabic(surah.getNameArabic())
                .nameLatin(surah.getNameLatin())
                .nameTranslation(surah.getNameTranslation())
                .totalAyahs(surah.getTotalAyahs())
                .revelationType(surah.getRevelationType().name())
                .description(surah.getDescription())
                .currentLanguage(languageCode)
                .basmala(basmala)
                .ayahs(ayahResponses)
                .build();
    }

    public AyahResponse toAyahResponse(Ayah entity) {
        return AyahResponse.builder()
                .id(entity.getId())
                .ayahNumber(entity.getAyahNumber())
                .textArabic(entity.getTextArabic())
                .juzNumber(entity.getJuzNumber())
                .pageNumber(entity.getPageNumber())
                .sajda(entity.getSajda())
                .build();
    }

    public AyahResponse toAyahResponse(Ayah entity, Translation translation) {
        return AyahResponse.builder()
                .id(entity.getId())
                .ayahNumber(entity.getAyahNumber())
                .textArabic(entity.getTextArabic())
                .juzNumber(entity.getJuzNumber())
                .pageNumber(entity.getPageNumber())
                .sajda(entity.getSajda())
                .translation(translation != null ? translation.getText() : null)
                .build();
    }

    public AyahDetailResponse toAyahDetailResponse(
            Ayah entity,
            Translation translation) {

        List<TranslationResponse> translations = translation != null ?
                List.of(toTranslationResponse(translation)) :
                List.of();

        return AyahDetailResponse.builder()
                .id(entity.getId())
                .surahNumber(entity.getSurah().getSurahNumber())
                .surahName(entity.getSurah().getNameLatin())
                .ayahNumber(entity.getAyahNumber())
                .textArabic(entity.getTextArabic())
                .juzNumber(entity.getJuzNumber())
                .pageNumber(entity.getPageNumber())
                .sajda(entity.getSajda())
                .translations(translations)
                .build();
    }

    public TranslationResponse toTranslationResponse(Translation entity) {
        return TranslationResponse.builder()
                .id(entity.getId())
                .languageCode(entity.getLanguage().getCode())
                .languageName(entity.getLanguage().getName())
                .translatorName(entity.getTranslatorName())
                .text(entity.getText())
                .footnotes(entity.getFootnotes())
                .build();
    }

    public LanguageResponse toLanguageResponse(Language entity) {
        return LanguageResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .nativeName(entity.getNativeName())
                .isActive(entity.getIsActive())
                .build();
    }

}
