package com.techdesk.tenant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Application-level bean declarations for the tenant-service.
 *
 * BCryptPasswordEncoder is declared here (not inside TenantService) so that:
 *   - It is a true singleton managed by Spring — one instance shared across the application.
 *   - It can be injected via constructor injection and mocked in unit tests.
 *   - Cost factor 12 is enforced in exactly one place.
 */
@Configuration
public class BeanConfig {

    /**
     * BCrypt password encoder with cost factor 12.
     * Cost factor 12 requires ~250ms per hash on modern hardware — strong enough
     * to resist brute-force attacks while remaining acceptable for login latency.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
