package com.quran.web.security;

import com.quran.web.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;
    private final UserRepository userRepository;

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String ip = getClientIp();
        if (ip == null) return;
        
        loginAttemptService.loginFailed(ip);
        
        String username = event.getAuthentication().getName();
        userRepository.updateLastLoginWithIp(username, ip);
        setLog(ip, username);
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String ip = getClientIp();
        if (ip == null) return;

        loginAttemptService.loginSucceeded(ip);

        String username = event.getAuthentication().getName();
        userRepository.updateLastLoginWithIp(username, ip);
        setLog(ip, username);
    }

    private static void setLog(String ip, String username) {
        log.debug("Recorded login IP {} for user {}", ip, username);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = getServletRequestContext();
            if (isABoolean(attrs)) return null;
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.debug("Could not extract IP from request context", e);
            return null;
        }
    }

    private static boolean isABoolean(ServletRequestAttributes attrs) {
        return attrs == null;
    }

    private static @Nullable ServletRequestAttributes getServletRequestContext() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }
}