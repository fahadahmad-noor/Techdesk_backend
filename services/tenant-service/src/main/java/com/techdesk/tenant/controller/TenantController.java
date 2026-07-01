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

// Thin controller for the 3 tenant management endpoints — all restricted to SUPER_ADMIN by SecurityConfig
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TenantResponse>> getAllTenants(
            @RequestParam(required = false) TenantStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(tenantService.getAllTenants(status, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TenantResponse> updateTenantStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantStatusRequest request) {

        return ResponseEntity.ok(tenantService.updateTenantStatus(id, request));
    }
}
