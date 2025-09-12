package com.ridesync.util;

import com.ridesync.model.User;
import com.ridesync.security.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class SecurityUtil {
    
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUser();
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
