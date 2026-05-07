package com.quran.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    @NotBlank(message = "Search query is required")
    @Size(min = 3, message = "Search query must be at least 3 characters")
    private String query;

    private String languageCode;  // search dalam bahasa tertentu

    private SearchType searchType;  // TEXT, TRANSLATION, TAFSIR

    @Min(0)
    private Integer page = 0;

    @Min(1)
    @Max(100)
    private Integer size = 20;
}
