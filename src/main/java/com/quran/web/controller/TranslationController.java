package com.quran.web.controller;

import com.quran.web.dto.response.LanguageResponse;
import com.quran.web.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/translations")
@RequiredArgsConstructor
@Slf4j
public class TranslationController {

    private final TranslationService translationService;

    /**
     * GET /translations/languages
     * Get available languages (view)
     */
    @GetMapping("/languages")
    public String getLanguagesView(Model model) {
        log.info("GET /translations/languages (View)");
        List<LanguageResponse> languages = translationService.getAvailableLanguages();
        model.addAttribute("languages", languages);
        return "translations/list";
    }
}
