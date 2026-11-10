package gov.tip.admin.securityaudit.constants;

/**
 * Role names as they appear in the Entra ID "roles" claim (without the ROLE_ prefix).
 * TODO: align with the actual TIP role names.
 */
public final class SecurityRoles {

    private SecurityRoles() {
    }

    public static final String AUDITOR = "AUD";
    public static final String SYSTEM_ADMIN = "SYS_ADMIN";

    /** May view the log (read-only for everyone). */
    public static final String[] VIEW_ROLES = {AUDITOR, SYSTEM_ADMIN};

    public static final String ROLES_CLAIM = "roles";
    public static final String ROLE_PREFIX = "ROLE_";
}
