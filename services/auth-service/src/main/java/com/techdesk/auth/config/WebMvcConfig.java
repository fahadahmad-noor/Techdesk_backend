package com.techdesk.auth.config;

import com.techdesk.auth.multitenancy.TenantInterceptor;
import com.techdesk.auth.repository.TenantRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Registers the TenantInterceptor so every request gets its schema set before hitting a controller
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantRepository tenantRepository;

    public WebMvcConfig(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantInterceptor(tenantRepository))
                .addPathPatterns("/**")
                // Exclude health checks — they don't need tenant context
                .excludePathPatterns("/actuator/**");
    }
}
