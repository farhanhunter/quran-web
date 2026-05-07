package com.quran.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkRequest {

    @NotNull(message = "Ayah ID is required")
    private Long ayahId;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
