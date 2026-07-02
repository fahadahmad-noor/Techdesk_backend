package com.techdesk.ticket.config;

import com.techdesk.ticket.multitenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Guarantees TenantContext is cleared after every request — prevents ThreadLocal leaks in the thread pool
@Component
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantCleanupInterceptor());
    }

    static class TenantCleanupInterceptor implements HandlerInterceptor {
        @Override
        public void afterCompletion(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull Object handler,
                                    Exception ex) {
            TenantContext.clear();
        }
    }
}
