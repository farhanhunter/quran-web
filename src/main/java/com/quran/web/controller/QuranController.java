package com.quran.web.controller;

import com.quran.web.dto.response.AyahDetailResponse;
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
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/quran")
@RequiredArgsConstructor
@Slf4j
public class QuranController {

    private final QuranService quranService;

    private static final Pattern SURAH_AYAH = Pattern.compile("^(\\d+)[:\\s](\\d+)$");

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q) {
        String query = q.trim();
        if (query.isEmpty()) return "redirect:/quran/surahs";

        Matcher m = SURAH_AYAH.matcher(query);
        if (m.matches()) {
            return "redirect:/quran/surah/" + m.group(1) + "#ayah-" + m.group(2);
        }

        if (query.matches("^\\d+$")) {
            int num = Integer.parseInt(query);
            if (num >= 1 && num <= 114) return "redirect:/quran/surah/" + num;
        }

        List<SurahResponse> results = quranService.searchSurahsByName(query);
        if (!results.isEmpty()) return "redirect:/quran/surah/" + results.get(0).getSurahNumber();

        return "redirect:/quran/surahs";
    }

    @GetMapping("/api/suggest")
    @ResponseBody
    public List<SurahResponse> suggest() {
        return quranService.getAllSurahs();
    }

    @GetMapping("/sajda")
    public String getSajdaAyahs(Model model) {
        model.addAttribute("ayahs", quranService.getSajdaAyahs());
        return "quran/sajda";
    }

    @GetMapping("/juz/{juz}")
    public String getJuzPage(@PathVariable int juz, Model model) {
        if (juz < 1 || juz > 30) return "redirect:/quran/juz/1";
        model.addAttribute("ayahs", quranService.getAyahsByJuz(juz));
        model.addAttribute("juz", juz);
        model.addAttribute("prevJuz", juz > 1 ? juz - 1 : null);
        model.addAttribute("nextJuz", juz < 30 ? juz + 1 : null);
        return "quran/juz";
    }

    @GetMapping("/page/{page}")
    public String getMushafPage(@PathVariable int page, Model model) {
        if (page < 1 || page > 604) return "redirect:/quran/page/1";
        model.addAttribute("ayahs", quranService.getAyahsByPage(page));
        model.addAttribute("page", page);
        model.addAttribute("prevPage", page > 1 ? page - 1 : null);
        model.addAttribute("nextPage", page < 604 ? page + 1 : null);
        return "quran/page";
    }

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
