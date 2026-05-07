package com.quran.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TafsirResponse {
    private Long id;
    private String sourceName;
    private String author;
    private String languageCode;
    private String text;
}
