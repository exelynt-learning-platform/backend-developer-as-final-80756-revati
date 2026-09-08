package com.booking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        return principal;
    }

    public Long currentUserId() {
        return currentUser().getId();
    }

    public boolean isAdmin() {
        return currentUser().getRole().name().equals("ADMIN");
    }
}
