package com.quran.web.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LastReadInfo {
    int surahNumber;
    String surahNameLatin;
    String surahNameArabic;
    int ayahNumber;
    String resumeUrl;
}
