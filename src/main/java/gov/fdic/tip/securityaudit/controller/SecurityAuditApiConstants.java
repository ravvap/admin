package gov.fdic.tip.securityaudit.constants;

public final class SecurityAuditApiConstants {

    private SecurityAuditApiConstants() {
    }

    public static final String BASE_PATH = "/api/v1/security-audit";
    public static final String SECURITY_TRANSACTIONS_PATH = "/security-transactions";

    public static final int DEFAULT_PAGE_SIZE = 15;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String MSG_DATE_RANGE_INVALID = "'from' date must be on or before 'to' date";
}
