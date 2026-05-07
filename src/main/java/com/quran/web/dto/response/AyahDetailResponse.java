package com.quran.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AyahDetailResponse {
    private Long id;
    private Integer surahNumber;
    private String surahName;
    private Integer ayahNumber;
    private String textArabic;
    private Integer juzNumber;
    private Integer pageNumber;
    private Boolean sajda;

    // Multiple translations
    private List<TranslationResponse> translations;

    // Multiple tafsirs
    private List<TafsirResponse> tafsirs;

    // Word by word
    private List<WordResponse> words;
}
