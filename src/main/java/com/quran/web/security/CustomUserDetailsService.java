package com.quran.web.security;

import com.quran.web.model.User;
import com.quran.web.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String clientIp = getClientIp();
        if (clientIp != null && loginAttemptService.isBlocked(clientIp)) {
            log.warn("Login blocked from IP {} — too many failed attempts", clientIp);
            throw new LockedException("Too many failed login attempts. Please try again later.");
        }

        User user = userRepository.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> {
                    log.warn("Login attempt for unknown identifier: {}", maskIdentifier(identifier));
                    return new UsernameNotFoundException("Invalid credentials");
                });

        log.debug("Loaded user: {} (active={})", user.getUsername(), user.getIsActive());
        return new CustomUserDetails(user);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 2) return "***";
        return identifier.charAt(0) + "*".repeat(identifier.length() - 2) + identifier.charAt(identifier.length() - 1);
    }
}
