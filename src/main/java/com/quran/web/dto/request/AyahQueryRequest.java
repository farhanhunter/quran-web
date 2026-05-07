package com.quran.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AyahQueryRequest {

    @NotNull(message = "Surah number is required")
    @Min(value = 1, message = "Surah number must be between 1 and 114")
    @Max(value = 114, message = "Surah number must be between 1 and 114")
    private Integer surahNumber;

    @Min(value = 1, message = "Ayah number must be positive")
    private Integer ayahNumber;  // optional, jika null = semua ayat

    private String languageCode;  // default: "id"

    private Boolean includeWords;  // default: false
    private Boolean includeTafsir;  // default: false
}
