package com.techdesk.ticket.security;

import com.techdesk.ticket.multitenancy.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Validates the Bearer JWT, hydrates Spring Security context, and sets TenantContext in one pass
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER.length());

        try {
            String userId   = jwtUtil.extractUserId(token);
            String role     = jwtUtil.extractRole(token);
            String tenantId = jwtUtil.extractTenantId(token);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Set TenantContext here so it's available before the first Hibernate session opens
            TenantContext.set(new TenantContext.TenantInfo(tenantId, tenantId));
            log.debug("Authenticated userId={} role={} tenant={}", userId, role, tenantId);

        } catch (JwtException ex) {
            log.warn("Invalid JWT on ticket-service: {}", ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}
