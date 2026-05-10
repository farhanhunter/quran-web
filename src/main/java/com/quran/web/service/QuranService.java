package com.quran.web.service;

import com.quran.web.dto.request.AyahQueryRequest;
import com.quran.web.dto.request.SearchRequest;
import com.quran.web.dto.response.*;

import java.util.List;

public interface QuranService {
    List<SurahResponse> getAllSurahs();
    List<SurahResponse> searchSurahsByName(String name);
    List<AyahDetailResponse> getSajdaAyahs();
    SurahDetailResponse getSurahDetail(Integer surahNumber, String languageCode);
    AyahDetailResponse getAyahDetail(AyahQueryRequest request);
    PageResponse<AyahResponse> searchAyahs(SearchRequest request);
}
