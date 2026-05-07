package com.quran.web.controller;

import com.quran.web.dto.response.LastReadInfo;
import com.quran.web.model.User;
import com.quran.web.repository.SurahRepository;
import com.quran.web.repository.UserRepository;
import com.quran.web.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SurahRepository surahRepository;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            // Re-query DB — session copy may be stale if last_read was updated after login
            userRepository.findById(userDetails.getUserId()).flatMap(this::buildLastReadInfo).ifPresent(info -> model.addAttribute("dbLastRead", info));
            model.addAttribute("loggedIn", true);
        } else {
            model.addAttribute("loggedIn", false);
        }
        return "index";
    }

    private java.util.Optional<LastReadInfo> buildLastReadInfo(User user) {
        if (user.getLastReadSurahNumber() == null) {
            return java.util.Optional.empty();
        }
        return surahRepository.findBySurahNumber(user.getLastReadSurahNumber())
                .map(surah -> LastReadInfo.builder()
                        .surahNumber(surah.getSurahNumber())
                        .surahNameLatin(surah.getNameLatin())
                        .surahNameArabic(surah.getNameArabic())
                        .ayahNumber(user.getLastReadAyahNumber())
                        .resumeUrl("/quran/surah/" + surah.getSurahNumber()
                                   + "#ayah-" + user.getLastReadAyahNumber())
                        .build());
    }
}
