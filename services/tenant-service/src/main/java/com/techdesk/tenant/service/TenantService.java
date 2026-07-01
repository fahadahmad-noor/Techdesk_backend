package com.techdesk.tenant.service;

import com.techdesk.tenant.dto.request.CreateTenantRequest;
import com.techdesk.tenant.dto.request.UpdateTenantStatusRequest;
import com.techdesk.tenant.dto.response.TenantResponse;
import com.techdesk.tenant.entity.Tenant;
import com.techdesk.tenant.entity.TenantPlan;
import com.techdesk.tenant.entity.TenantStatus;
import com.techdesk.tenant.exception.TenantAlreadyExistsException;
import com.techdesk.tenant.exception.TenantNotFoundException;
import com.techdesk.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.stream.Collectors;

// Handles the full tenant onboarding flow — schema creation, migrations, admin user, and email
@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    // Excludes visually ambiguous characters like 0/O and l/I to reduce copy-paste errors
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final SchemaProvisioningService schemaProvisioningService;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository,
                         SchemaProvisioningService schemaProvisioningService,
                         EmailService emailService,
                         JdbcTemplate jdbcTemplate,
                         BCryptPasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.schemaProvisioningService = schemaProvisioningService;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    // Creates schema, runs migrations, creates admin user, sends email — rolls back fully on any failure
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        log.info("Processing tenant creation request for: {}", request.getCompanyName());

        if (tenantRepository.existsByName(request.getCompanyName())) {
            throw new TenantAlreadyExistsException(request.getCompanyName());
        }

        String schemaName = generateSchemaName(request.getCompanyName());

        // Also check the generated schema name — "Acme Corp" and "Acme-Corp" both map to tenant_acme_corp
        if (tenantRepository.existsBySchemaName(schemaName)) {
            throw new TenantAlreadyExistsException(request.getCompanyName());
        }

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName(request.getCompanyName());
        tenant.setSchemaName(schemaName);
        tenant.setStatus(TenantStatus.PENDING);
        tenant.setPlan(TenantPlan.valueOf(request.getPlan()));

        // Catches the rare case where two concurrent requests both pass the name check
        try {
            tenantRepository.save(tenant);
        } catch (DataIntegrityViolationException ex) {
            throw new TenantAlreadyExistsException(request.getCompanyName());
        }

        schemaProvisioningService.provisionSchema(schemaName);

        String temporaryPassword = generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(temporaryPassword);

        createDefaultAdminUser(schemaName, request, hashedPassword);

        // Email failure is non-fatal — tenant is already active at this point
        emailService.sendWelcomeEmail(
                request.getAdminEmail(),
                request.getAdminFirstName(),
                request.getCompanyName(),
                temporaryPassword
        );

        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        log.info("Tenant '{}' successfully onboarded with schema '{}'.", request.getCompanyName(), schemaName);

        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSchemaName(),
                tenant.getStatus(),
                tenant.getPlan(),
                request.getAdminEmail(),
                tenant.getCreatedAt()
        );
    }

    // Returns paginated tenants, optionally filtered by status
    @Transactional(readOnly = true)
    public Page<TenantResponse> getAllTenants(TenantStatus status, Pageable pageable) {
        Page<Tenant> tenants = (status != null)
                ? tenantRepository.findByStatus(status, pageable)
                : tenantRepository.findAll(pageable);

        return tenants.map(t -> new TenantResponse(
                t.getId(), t.getName(), t.getSchemaName(),
                t.getStatus(), t.getPlan(), null, t.getCreatedAt()
        ));
    }

    // Updates tenant to ACTIVE or SUSPENDED — dirty-checking flushes the change automatically
    @Transactional
    public TenantResponse updateTenantStatus(UUID tenantId, UpdateTenantStatusRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId.toString()));

        TenantStatus newStatus = TenantStatus.valueOf(request.getStatus());
        tenant.setStatus(newStatus);

        tenantRepository.save(tenant);
        
        log.info("Tenant '{}' status updated to {}.", tenant.getName(), newStatus);

        return new TenantResponse(
                tenant.getId(), tenant.getName(), tenant.getSchemaName(),
                tenant.getStatus(), tenant.getPlan(), null, tenant.getCreatedAt()
        );
    }

    // Converts company name to a valid PostgreSQL schema name e.g. "Acme Corp" -> "tenant_acme_corp"
    String generateSchemaName(String companyName) {
        String normalized = companyName
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return "tenant_" + normalized;
    }

    // Generates a 14-char secure random password using SecureRandom
    private String generateTemporaryPassword() {
        return SECURE_RANDOM
                .ints(14, 0, TEMP_PASSWORD_CHARS.length())
                .mapToObj(i -> String.valueOf(TEMP_PASSWORD_CHARS.charAt(i)))
                .collect(Collectors.joining());
    }

    // Inserts the first COMPANY_ADMIN directly into the new schema using raw SQL — JPA routing comes in Phase 3.4
    private void createDefaultAdminUser(String schemaName, CreateTenantRequest request, String hashedPassword) {
        String sql = String.format(
            "INSERT INTO \"%s\".users (id, email, password_hash, first_name, last_name, role, status) " +
            "VALUES (?, ?, ?, ?, ?, 'COMPANY_ADMIN', 'INVITED')",
            schemaName
        );

        jdbcTemplate.update(sql,
                UUID.randomUUID(),
                request.getAdminEmail(),
                hashedPassword,
                request.getAdminFirstName(),
                request.getAdminLastName()
        );

        log.info("Default COMPANY_ADMIN created in schema '{}' for email '{}'.", schemaName, request.getAdminEmail());
    }
}
