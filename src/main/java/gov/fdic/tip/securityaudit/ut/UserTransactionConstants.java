package gov.tip.admin.securityaudit.constants;

import java.time.ZoneId;
import java.util.Map;

public final class UserTransactionConstants {

    private UserTransactionConstants() {
    }

    // ---- API paths ----
    public static final String BASE_PATH = "/api/v1/security-audit/user-transactions";
    public static final String FILTER_OPTIONS_PATH = "/filter-options";

    // ---- Paging / sorting ----
    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "changedAt";

    /** API sort property -> SQL expression over the txn CTE. Anything not listed here is ignored. */
    public static final Map<String, String> SORT_COLUMNS = Map.of(
            "changedAt", "changed_at",
            "userId", "user_id",
            "certNumber", "LPAD(cert_number, 12, '0')",
            "assessmentPeriod", "assessment_period",
            "fieldName", "field_name",
            "transactionType", "transaction_type",
            "reasonCode", "reason_code");
    public static final String SORT_TIE_BREAKER = "snapshot_pk DESC, field_name ASC";

    // ---- Field limits ----
    public static final int USER_ID_MAX = 200;
    public static final int FIELD_NAME_MAX = 191;
    public static final int VALUE_MAX = 4000;
    public static final String TRANSACTION_TYPE_PATTERN = "^[A-Za-z_]{1,30}$";
    public static final String ASSESSMENT_PERIOD_PATTERN = "^\\d{4} Q[1-4]$";
    public static final char LIKE_ESCAPE = '!';

    // ---- Display ----
    public static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");

    // ---- Messages ----
    public static final String MSG_INVALID_DATE_RANGE = "'changedFrom' must not be after 'changedTo'";

    // ---- Configuration ----
    public static final String PROPERTIES_PREFIX = "tip.security-audit.user-transactions";
}
