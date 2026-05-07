package com.quran.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkResponse {
    private Long id;
    private Long ayahId;
    private Integer surahNumber;
    private String surahName;
    private Integer ayahNumber;
    private String ayahTextArabic;
    private String notes;
    private LocalDateTime createdAt;
}
