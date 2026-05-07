package com.quran.web.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptRecord> cache = new ConcurrentHashMap<>();

    private record AttemptRecord(int count, Instant blockedUntil) {}

    public void loginFailed(String ip) {
        cache.compute(ip, (key, existing) -> {
            int newCount = (existing == null ? 0 : existing.count()) + 1;
            Instant blockedUntil = newCount >= MAX_ATTEMPTS ? Instant.now().plus(BLOCK_DURATION) : null;
            if (newCount == MAX_ATTEMPTS) {
                log.warn("IP {} blocked after {} failed login attempts", ip, newCount);
            }
            return new AttemptRecord(newCount, blockedUntil);
        });
    }

    public void loginSucceeded(String ip) {
        cache.remove(ip);
    }

    public boolean isBlocked(String ip) {
        AttemptRecord record = cache.get(ip);
        if (record == null || record.blockedUntil() == null) return false;
        if (Instant.now().isAfter(record.blockedUntil())) {
            cache.remove(ip);
            return false;
        }
        return true;
    }
}