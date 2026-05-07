package com.quran.web.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class HealthStatus {
    private String status;
    private LocalDateTime timestamp;
}
