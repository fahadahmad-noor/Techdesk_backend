package com.techdesk.auth.multitenancy;

import com.techdesk.auth.entity.Tenant;
import com.techdesk.auth.exception.TenantNotFoundException;
import com.techdesk.auth.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

// Reads X-Tenant-ID header, resolves the schema name, and stores it in TenantContextHolder for the request
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantRepository tenantRepository;

    public TenantInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        String tenantIdentifier = request.getHeader(TENANT_HEADER);

        if (tenantIdentifier != null && !tenantIdentifier.isBlank()) {
            Tenant tenant = tenantRepository.findByName(tenantIdentifier)
                    .orElseThrow(() -> new TenantNotFoundException(
                            "Tenant not found: " + tenantIdentifier));

            if (!"ACTIVE".equals(tenant.getStatus())) {
                throw new TenantNotFoundException(
                        "Tenant is not active: " + tenantIdentifier);
            }

            TenantContextHolder.setCurrentSchema(tenant.getSchemaName());
            log.debug("Resolved tenant '{}' → schema '{}'", tenantIdentifier, tenant.getSchemaName());
        } else {
            log.debug("No X-Tenant-ID header found, using public schema");
        }

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        TenantContextHolder.clear();
    }
}
