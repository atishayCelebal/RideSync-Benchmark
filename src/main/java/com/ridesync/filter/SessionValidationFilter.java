package com.ridesync.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.config.SessionManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionValidationFilter extends OncePerRequestFilter {
    
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Only apply to location update endpoints
        if (isLocationUpdateRequest(request)) {
            // Wrap the request to cache the content
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            
            try {
                LocationUpdateDto locationDto = extractLocationDto(wrappedRequest);
                if (locationDto != null) {
                    UUID userId = locationDto.getUserId();
                    UUID rideId = locationDto.getRideId();
                    
                    // Always allow location updates and update session timestamp
                    // This ensures continuous location updates work while still tracking sessions
                    sessionManager.createOrUpdateSession(userId, rideId);
                }
            } catch (Exception e) {
                // If we can't parse the request, let it through to be handled by validation
                logger.warn("Could not parse location update request for session validation", e);
            }
            
            // Continue with the wrapped request
            filterChain.doFilter(wrappedRequest, response);
        } else {
            // For non-location update requests, proceed normally
            filterChain.doFilter(request, response);
        }
    }
    
    private boolean isLocationUpdateRequest(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && 
               request.getRequestURI().contains("/location/update");
    }
    
    private LocationUpdateDto extractLocationDto(ContentCachingRequestWrapper request) throws IOException {
        if (request.getContentType() != null && 
            request.getContentType().contains(MediaType.APPLICATION_JSON_VALUE)) {
            
            // Get the cached content as byte array
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                return objectMapper.readValue(content, LocationUpdateDto.class);
            }
        }
        return null;
    }
    
}