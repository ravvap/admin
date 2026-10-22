package com.fdic.tip.usertransactionlog.constants;

/**
 * Role names as they appear in the Entra ID "roles" claim (without the ROLE_ prefix).
 * TODO: align with the actual TIP role names.
 */
public final class SecurityRoles {

    private SecurityRoles() {
    }

    public static final String AUDITOR = "AUD";
    public static final String SYSTEM_ADMIN = "SYS_ADMIN";

    /** Roles that may view the log. */
    public static final String[] VIEW_ROLES = {AUDITOR, SYSTEM_ADMIN};

    /** Roles that may download (CSV export). Auditors can look but not download. */
    public static final String[] EXPORT_ROLES = {SYSTEM_ADMIN};

    public static final String ROLES_CLAIM = "roles";
    public static final String ROLE_PREFIX = "ROLE_";
}
