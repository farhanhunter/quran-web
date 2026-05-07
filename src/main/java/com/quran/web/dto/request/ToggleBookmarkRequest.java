package com.quran.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ToggleBookmarkRequest {

    @NotNull
    @Min(1) @Max(114)
    private Integer surahNumber;

    @NotNull
    @Min(1)
    private Integer ayahNumber;

    @Size(max = 1000)
    private String notes;
}
