package com.quran.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageResponse {
    private Long id;
    private String code;
    private String name;
    private String nativeName;
    private Boolean isActive;
}

