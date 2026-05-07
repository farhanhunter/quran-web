package com.quran.web.controller;

import com.quran.web.dto.response.SurahDetailResponse;
import com.quran.web.dto.response.SurahResponse;
import com.quran.web.service.QuranService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/quran")
@RequiredArgsConstructor
@Slf4j
public class QuranController {

    private final QuranService quranService;

    /**
     * GET /quran/surahs
     * Get list of all surahs (view)
     */
    @GetMapping("/surahs")
    public String getAllSurahsView(Model model) {
        log.info("GET /quran/surahs (View)");
        List<SurahResponse> surahs = quranService.getAllSurahs();
        model.addAttribute("surahs", surahs);
        return "quran/surahs";
    }

    /**
     * GET /quran/surah/{surahNumber}
     * Get surah detail with ayahs (view)
     */
    @GetMapping("/surah/{surahNumber}")
    public String getSurahDetailView(
            @PathVariable Integer surahNumber,
            @RequestParam(required = false, defaultValue = "en") String languageCode,
            Model model) {

        log.info("GET /quran/surah/{} with language: {} (View)", surahNumber, languageCode);
        SurahDetailResponse surah = quranService.getSurahDetail(surahNumber, languageCode);
        model.addAttribute("surah", surah);
        return "quran/surah-detail";
    }
}
