package com.quran.web.controller;

import com.quran.web.dto.request.UserRegistrationRequest;
import com.quran.web.exception.InvalidRequestException;
import com.quran.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Invalid username/email or password.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new UserRegistrationRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") UserRegistrationRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(request);
            redirectAttributes.addFlashAttribute("registrationSuccess",
                    "Account created! Please log in.");
            return "redirect:/auth/login";
        } catch (InvalidRequestException e) {
            model.addAttribute("registerError", e.getMessage());
            return "auth/register";
        }
    }
}
