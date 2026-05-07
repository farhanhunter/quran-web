package com.quran.web.controller;

import com.quran.web.dto.response.HealthStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public String health(Model model) {
        HealthStatus status = HealthStatus.builder()
                .status("UP")
                .timestamp(java.time.LocalDateTime.now())
                .build();

        model.addAttribute("health", status);
        return "health/status";
    }
}
