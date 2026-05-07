package com.quran.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadingProgressRequest {

    @NotNull
    @Min(1) @Max(114)
    private Integer surahNumber;

    @NotNull
    @Min(1)
    private Integer ayahNumber;
}
