package com.guerrero.timeproject.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class RateLimitingFilter implements Filter {

    private final Map<String, List<Long>> requestTimesMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long ONE_MINUTE_MS = 60000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        
        // Only rate limit /api/auth/** endpoints to protect against login brute-forcing
        if (path != null && path.startsWith("/api/auth")) {
            String ip = httpRequest.getRemoteAddr();
            long now = Instant.now().toEpochMilli();

            requestTimesMap.putIfAbsent(ip, new CopyOnWriteArrayList<>());
            List<Long> times = requestTimesMap.get(ip);

            synchronized (times) {
                // Remove timestamps older than 1 minute
                times.removeIf(time -> now - time > ONE_MINUTE_MS);

                if (times.size() >= MAX_REQUESTS_PER_MINUTE) {
                    httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    httpResponse.setContentType("application/json");
                    httpResponse.setCharacterEncoding("UTF-8");
                    httpResponse.getWriter().write("{\"error\": \"Too many requests. Please try again in a minute. (Bot protection active)\"}");
                    return;
                }

                times.add(now);
            }
        }

        chain.doFilter(request, response);
    }
}
