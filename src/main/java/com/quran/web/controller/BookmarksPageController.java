package com.quran.web.controller;

import com.quran.web.security.CustomUserDetails;
import com.quran.web.service.UserBookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class BookmarksPageController {

    private final UserBookmarkService userBookmarkService;

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("bookmarks",
                userBookmarkService.getAll(userDetails.getUserId()));
        return "bookmarks";
    }
}
