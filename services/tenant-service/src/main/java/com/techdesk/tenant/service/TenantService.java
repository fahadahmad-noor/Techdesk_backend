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

/**
 * Core business logic for tenant lifecycle management.
 *
 * Orchestrates the full tenant onboarding flow:
 *   1. Validates that the company name is unique.
 *   2. Generates a safe PostgreSQL schema name from the company name.
 *   3. Inserts the tenant record into public.tenants with PENDING status.
 *   4. Delegates schema creation and Flyway migrations to SchemaProvisioningService.
 *   5. Creates the default COMPANY_ADMIN user inside the new tenant schema.
 *   6. Sends the welcome email via EmailService.
 *   7. Updates the tenant status to ACTIVE.
 *
 * The challenge task (transactional rollback) is handled inside SchemaProvisioningService:
 * if provisioning fails, the schema is dropped and this method re-throws the exception,
 * causing Spring's @Transactional to roll back the public.tenants record insertion as well.
 */
@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    /**
     * Character set used when generating temporary passwords.
     * Excludes visually ambiguous characters (0, O, l, I) to reduce
     * copy-paste errors when admins read the password from their email.
     */
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";

    /** Cryptographically secure random source for temporary password generation. */
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

    /**
     * Onboards a new company onto the TechDesk platform.
     *
     * This is a transactional operation. If schema provisioning fails, Spring will roll back
     * the public.tenants insert, and SchemaProvisioningService will drop the partial schema.
     *
     * @param request the validated onboarding request from the Super Admin
     * @return a TenantResponse containing the new tenant's details
     * @throws TenantAlreadyExistsException if the company name is already taken
     */
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        log.info("Processing tenant creation request for: {}", request.getCompanyName());

        // Guard: reject duplicate company names before doing any database work.
        if (tenantRepository.existsByName(request.getCompanyName())) {
            throw new TenantAlreadyExistsException(request.getCompanyName());
        }

        // Derive a safe, unique schema name from the company name.
        String schemaName = generateSchemaName(request.getCompanyName());

        // Insert the tenant record with PENDING status so it is visible during provisioning.
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName(request.getCompanyName());
        tenant.setSchemaName(schemaName);
        tenant.setStatus(TenantStatus.PENDING);
        tenant.setPlan(TenantPlan.valueOf(request.getPlan()));

        // Wrap save() to handle the rare concurrent-creation race condition (TOCTOU).
        // If two simultaneous requests pass the existsByName check, the DB unique constraint
        // on the name column will reject the second insert — we translate that to our
        // clean domain exception so callers always receive a 409, never a raw 500.
        try {
            tenantRepository.save(tenant);
        } catch (DataIntegrityViolationException ex) {
            throw new TenantAlreadyExistsException(request.getCompanyName());
        }

        // Provision the isolated PostgreSQL schema and run all tenant Flyway migrations.
        // If this throws, @Transactional rolls back the tenant record and the schema is dropped.
        schemaProvisioningService.provisionSchema(schemaName);

        // Generate a cryptographically secure temporary password for the first Company Admin login.
        String temporaryPassword = generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(temporaryPassword);

        // Insert the default COMPANY_ADMIN user directly into the new tenant schema.
        createDefaultAdminUser(schemaName, request, hashedPassword);

        // Dispatch the welcome email. Failure here is non-fatal — the tenant is already operational.
        emailService.sendWelcomeEmail(
                request.getAdminEmail(),
                request.getAdminFirstName(),
                request.getCompanyName(),
                temporaryPassword
        );

        // Mark the tenant as fully operational.
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

    /**
     * Returns a paginated list of all tenants on the platform.
     * Supports optional filtering by status (ACTIVE, SUSPENDED, PENDING).
     *
     * @param status   optional status filter; if null, all tenants are returned
     * @param pageable pagination and sorting parameters
     * @return a Page of TenantResponse objects
     */
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

    /**
     * Updates the lifecycle status of an existing tenant (ACTIVE or SUSPENDED).
     * When suspended, the tenant's users will be denied login by the auth-service.
     *
     * @param tenantId the UUID of the tenant to update
     * @param request  the status change request
     * @return the updated TenantResponse
     * @throws TenantNotFoundException if no tenant exists with the given ID
     */
    @Transactional
    public TenantResponse updateTenantStatus(UUID tenantId, UpdateTenantStatusRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId.toString()));

        TenantStatus newStatus = TenantStatus.valueOf(request.getStatus());
        tenant.setStatus(newStatus);
        // No explicit save() needed: the entity is JPA-managed within this @Transactional
        // boundary. Hibernate's dirty-checking will flush the status change automatically
        // on transaction commit.

        log.info("Tenant '{}' status updated to {}.", tenant.getName(), newStatus);

        return new TenantResponse(
                tenant.getId(), tenant.getName(), tenant.getSchemaName(),
                tenant.getStatus(), tenant.getPlan(), null, tenant.getCreatedAt()
        );
    }

    /**
     * Converts a company name into a safe PostgreSQL schema name.
     *
     * Rules applied:
     *   - Converted to lowercase
     *   - All non-alphanumeric characters replaced with underscores
     *   - Consecutive underscores collapsed to a single underscore
     *   - Prefixed with "tenant_" to namespace all tenant schemas clearly
     *
     * Example: "Acme Corp & Partners!" → "tenant_acme_corp_partners"
     *
     * @param companyName the raw company name from the onboarding request
     * @return a valid, namespaced PostgreSQL schema name
     */
    String generateSchemaName(String companyName) {
        String normalized = companyName
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return "tenant_" + normalized;
    }

    /**
     * Generates a 14-character cryptographically secure temporary password.
     *
     * Uses SecureRandom (not UUID.randomUUID which uses java.util.Random internally)
     * and a curated character set that excludes visually ambiguous characters
     * (e.g., 0/O, l/I/1) to reduce copy-paste errors when admins read the email.
     *
     * @return a random 14-character password string
     */
    private String generateTemporaryPassword() {
        return SECURE_RANDOM
                .ints(14, 0, TEMP_PASSWORD_CHARS.length())
                .mapToObj(i -> String.valueOf(TEMP_PASSWORD_CHARS.charAt(i)))
                .collect(Collectors.joining());
    }

    /**
     * Inserts the default COMPANY_ADMIN user directly into the new tenant schema.
     *
     * This uses JdbcTemplate with a fully qualified schema-prefixed table reference
     * because JPA/Hibernate multi-tenancy routing is not yet configured (that is Phase 3.4).
     * The user is created with INVITED status so the system can track that they have not yet
     * changed their temporary password.
     */
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
