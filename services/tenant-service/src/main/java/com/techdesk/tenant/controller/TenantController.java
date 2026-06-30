package com.techdesk.tenant.controller;

import com.techdesk.tenant.dto.request.CreateTenantRequest;
import com.techdesk.tenant.dto.request.UpdateTenantStatusRequest;
import com.techdesk.tenant.dto.response.TenantResponse;
import com.techdesk.tenant.entity.TenantStatus;
import com.techdesk.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing the three tenant management endpoints.
 *
 * This controller contains zero business logic — it delegates entirely to TenantService.
 * All endpoints require the SUPER_ADMIN role, enforced by SecurityConfig.
 *
 * Endpoints:
 *   POST  /api/tenants              — Onboard a new company
 *   GET   /api/tenants              — List all companies with pagination and optional status filter
 *   PATCH /api/tenants/{id}/status  — Suspend or reactivate a company
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Onboards a new company onto the TechDesk platform.
     * Creates the isolated database schema, runs all migrations, creates the Company Admin,
     * and dispatches the welcome email — all in a single transactional operation.
     *
     * @param request the validated onboarding payload
     * @return 201 Created with the new tenant's details
     */
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns a paginated list of all tenants on the platform.
     * The optional 'status' query parameter filters results by lifecycle state.
     *
     * @param status   optional filter (ACTIVE, SUSPENDED, or PENDING)
     * @param pageable pagination parameters (page, size, sort) — defaults to page 0, size 20
     * @return 200 OK with a paginated list of tenants
     */
    @GetMapping
    public ResponseEntity<Page<TenantResponse>> getAllTenants(
            @RequestParam(required = false) TenantStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(tenantService.getAllTenants(status, pageable));
    }

    /**
     * Updates the lifecycle status of an existing tenant.
     * Used by the Super Admin to suspend a company (e.g., subscription lapsed)
     * or reactivate a previously suspended account.
     *
     * @param id      the UUID of the tenant to update
     * @param request the validated status change payload
     * @return 200 OK with the updated tenant details
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TenantResponse> updateTenantStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantStatusRequest request) {

        return ResponseEntity.ok(tenantService.updateTenantStatus(id, request));
    }
}
