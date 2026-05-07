package com.quran.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurahResponse {
    private Long id;
    private Integer surahNumber;
    private String nameArabic;
    private String nameLatin;
    private String nameTranslation;
    private Integer totalAyahs;
    private String revelationType;
    private Integer revelationOrder;
}
