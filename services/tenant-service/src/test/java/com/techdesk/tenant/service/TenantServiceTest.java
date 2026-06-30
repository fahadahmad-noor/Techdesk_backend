package com.techdesk.tenant.service;

import com.techdesk.tenant.dto.request.CreateTenantRequest;
import com.techdesk.tenant.entity.Tenant;
import com.techdesk.tenant.entity.TenantPlan;
import com.techdesk.tenant.entity.TenantStatus;
import com.techdesk.tenant.exception.SchemaProvisioningException;
import com.techdesk.tenant.exception.TenantAlreadyExistsException;
import com.techdesk.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantService.
 *
 * All external collaborators (repository, schema provisioning, email, JDBC) are mocked.
 * These tests verify business logic in complete isolation — no database or network is required.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SchemaProvisioningService schemaProvisioningService;

    @Mock
    private EmailService emailService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    /**
     * Use @Spy (real object) rather than @Mock for BCryptPasswordEncoder.
     * A mock would return null from encode(), causing a NullPointerException
     * when TenantService tries to insert the hashed password.
     * A spy uses the actual BCrypt implementation — correct behaviour in tests.
     */
    @Spy
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4); // cost 4 for fast tests

    @InjectMocks
    private TenantService tenantService;

    private CreateTenantRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTenantRequest();
        validRequest.setCompanyName("Acme Corp");
        validRequest.setPlan("PREMIUM");
        validRequest.setAdminEmail("admin@acme.com");
        validRequest.setAdminFirstName("John");
        validRequest.setAdminLastName("Smith");
    }

    @Test
    @DisplayName("createTenant — throws TenantAlreadyExistsException when company name is already taken")
    void createTenant_duplicateName_throwsException() {
        when(tenantRepository.existsByName("Acme Corp")).thenReturn(true);

        assertThrows(TenantAlreadyExistsException.class, () -> tenantService.createTenant(validRequest));

        verify(schemaProvisioningService, never()).provisionSchema(any());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTenant — saves tenant record and delegates to SchemaProvisioningService on success")
    void createTenant_validRequest_savesAndProvisionsSchema() {
        when(tenantRepository.existsByName("Acme Corp")).thenReturn(false);

        Tenant savedTenant = new Tenant();
        savedTenant.setId(UUID.randomUUID());
        savedTenant.setName("Acme Corp");
        savedTenant.setSchemaName("tenant_acme_corp");
        savedTenant.setStatus(TenantStatus.ACTIVE);
        savedTenant.setPlan(TenantPlan.PREMIUM);

        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        doNothing().when(schemaProvisioningService).provisionSchema(anyString());
        doNothing().when(emailService).sendWelcomeEmail(any(), any(), any(), any());
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        var response = tenantService.createTenant(validRequest);

        assertNotNull(response);
        verify(schemaProvisioningService, times(1)).provisionSchema("tenant_acme_corp");
        verify(emailService, times(1)).sendWelcomeEmail(eq("admin@acme.com"), eq("John"), eq("Acme Corp"), anyString());
    }

    @Test
    @DisplayName("createTenant — propagates SchemaProvisioningException when Flyway migration fails")
    void createTenant_provisioningFails_throwsSchemaProvisioningException() {
        when(tenantRepository.existsByName("Acme Corp")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(new Tenant());
        doThrow(new SchemaProvisioningException("tenant_acme_corp", new RuntimeException("Flyway failed")))
                .when(schemaProvisioningService).provisionSchema(anyString());

        assertThrows(SchemaProvisioningException.class, () -> tenantService.createTenant(validRequest));

        verify(emailService, never()).sendWelcomeEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("generateSchemaName — converts company name to safe, namespaced schema name")
    void generateSchemaName_variousInputs_producesCorrectSchemaName() {
        assertEquals("tenant_acme_corp", tenantService.generateSchemaName("Acme Corp"));
        assertEquals("tenant_acme_corp_partners", tenantService.generateSchemaName("Acme Corp & Partners!"));
        assertEquals("tenant_nova_technologies", tenantService.generateSchemaName("Nova Technologies"));
        assertEquals("tenant_ats", tenantService.generateSchemaName("ATS"));
        assertEquals("tenant_test_company_123", tenantService.generateSchemaName("Test  Company  123"));
    }

    @Test
    @DisplayName("updateTenantStatus — throws TenantNotFoundException when tenant ID does not exist")
    void updateTenantStatus_nonExistentId_throwsException() {
        when(tenantRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        var request = new com.techdesk.tenant.dto.request.UpdateTenantStatusRequest();
        request.setStatus("SUSPENDED");

        assertThrows(com.techdesk.tenant.exception.TenantNotFoundException.class,
                () -> tenantService.updateTenantStatus(UUID.randomUUID(), request));
    }

    @Test
    @DisplayName("updateTenantStatus — updates and persists the new status correctly")
    void updateTenantStatus_validRequest_updatesStatus() {
        Tenant existing = new Tenant();
        existing.setId(UUID.randomUUID());
        existing.setName("Acme Corp");
        existing.setSchemaName("tenant_acme_corp");
        existing.setStatus(TenantStatus.ACTIVE);
        existing.setPlan(TenantPlan.PREMIUM);

        when(tenantRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(existing);

        var request = new com.techdesk.tenant.dto.request.UpdateTenantStatusRequest();
        request.setStatus("SUSPENDED");

        var response = tenantService.updateTenantStatus(existing.getId(), request);

        assertEquals(TenantStatus.SUSPENDED, response.getStatus());
        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        assertEquals(TenantStatus.SUSPENDED, captor.getValue().getStatus());
    }
}
