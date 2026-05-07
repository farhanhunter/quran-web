package com.quran.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationResponse {
    private Long id;
    private String languageCode;
    private String languageName;
    private String translatorName;
    private String text;
    private String footnotes;
}
