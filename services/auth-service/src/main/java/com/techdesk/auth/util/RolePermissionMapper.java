package com.techdesk.auth.util;

import java.util.List;
import java.util.Map;

/**
 * Maps user roles to their corresponding permission lists.
 *
 * This is a Phase 3.2 temporary solution because the full permissions table
 * does not exist until Phase 4.1 (Week 2 RBAC implementation).
 *
 * TODO Phase 4.1: Replace this class with a DB-driven PermissionService
 *                 that reads from the permissions table via PermissionRepository.
 */
public final class RolePermissionMapper {

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
        "SUPER_ADMIN", List.of(
            "MANAGE_TENANTS", "VIEW_ALL_TENANTS", "SUSPEND_TENANT",
            "VIEW_SYSTEM_HEALTH", "VIEW_ALL_AUDIT_LOGS"
        ),
        "COMPANY_ADMIN", List.of(
            "MANAGE_USERS", "MANAGE_DEPARTMENTS", "MANAGE_TICKET_CATEGORIES",
            "MANAGE_SLA_POLICIES", "VIEW_REPORTS", "VIEW_AUDIT_LOGS",
            "INVITE_USER", "SUSPEND_USER"
        ),
        "IT_MANAGER", List.of(
            "VIEW_ALL_TICKETS", "ASSIGN_TICKETS", "APPROVE_GADGETS",
            "REJECT_GADGETS", "MANAGE_ASSETS", "VIEW_REPORTS",
            "VIEW_AUDIT_LOGS", "MANAGE_TICKET_CATEGORIES"
        ),
        "IT_STAFF", List.of(
            "VIEW_ASSIGNED_TICKETS", "UPDATE_TICKET_STATUS",
            "ADD_TICKET_COMMENTS", "VIEW_ASSETS", "ADD_INTERNAL_COMMENTS"
        ),
        "EMPLOYEE", List.of(
            "CREATE_TICKET", "VIEW_OWN_TICKETS", "ADD_TICKET_COMMENTS",
            "CREATE_GADGET_REQUEST", "VIEW_OWN_GADGET_REQUESTS",
            "VIEW_OWN_NOTIFICATIONS", "VIEW_OWN_ASSETS"
        ),
        "AUDITOR", List.of(
            "VIEW_AUDIT_LOGS", "VIEW_ALL_TICKETS", "VIEW_REPORTS",
            "VERIFY_AUDIT_INTEGRITY"
        )
    );

    private RolePermissionMapper() {
        // Utility class — not instantiable
    }

    /**
     * Returns the permission list for a given role.
     *
     * @param role the role string (e.g. "EMPLOYEE")
     * @return list of permission strings; empty list if role is unknown
     */
    public static List<String> getPermissions(String role) {
        return ROLE_PERMISSIONS.getOrDefault(role, List.of());
    }
}
