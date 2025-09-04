package com.ridesync.util;

import com.ridesync.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class SecurityUtil {
    
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new IllegalStateException("No authenticated user found");
    }
    
    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }
}
