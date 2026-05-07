package com.quran.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordResponse {
    private Long id;
    private Integer position;
    private String textArabic;
    private String transliteration;
    private String translation;  // dalam bahasa yang dipilih
    private String rootWord;
}
