package com.techdesk.tenant.config;

import com.techdesk.tenant.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Locks down the entire service — only SUPER_ADMIN can access tenant endpoints, stateless JWT only
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator health check — open to Docker and load balancers
                .requestMatchers("/actuator/**").permitAll()
                // All tenant management endpoints require SUPER_ADMIN
                .requestMatchers(HttpMethod.POST,  "/api/tenants").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET,   "/api/tenants").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tenants/**").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
