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
public class AyahResponse {
    private Long id;
    private Integer ayahNumber;
    private String textArabic;
    private Integer juzNumber;
    private Integer pageNumber;
    private Boolean sajda;

    // Optional fields
    private String translation;  // terjemahan dalam bahasa yang dipilih
    private String tafsir;        // tafsir jika diminta
    private List<WordResponse> words;  // word-by-word jika diminta
}
