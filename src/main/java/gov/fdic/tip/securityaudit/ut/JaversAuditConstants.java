package gov.tip.admin.securityaudit.constants;

import java.util.List;

/**
 * How the Javers tables (jv_snapshot, jv_global_id, jv_commit, jv_commit_property) map to the
 * User Transaction Log. Adjust the property names here if TIP stores them differently.
 */
public final class JaversAuditConstants {

    private JaversAuditConstants() {
    }

    /** Shown when cert #, assessment period, reason code or description is not available. */
    public static final String NOT_APPLICABLE = "Not applicable";

    // jv_commit_property names ("saved with the change"); state JSON keys are used as a fallback.
    // "transactionType" lets a producer record types Javers cannot derive (e.g. READ).
    public static final String COMMIT_PROP_TRANSACTION_TYPE = "transactionType";
    public static final String COMMIT_PROP_CERT_NUMBER = "certNumber";
    public static final String COMMIT_PROP_ASSESSMENT_PERIOD = "assessmentPeriod";
    public static final String COMMIT_PROP_REASON_CODE = "reasonCode";
    public static final String COMMIT_PROP_REASON_DESCRIPTION = "reasonDescription";

    /**
     * Properties that are technical or already shown in their own column. They are never listed as a
     * changed "Field".
     */
    public static final List<String> EXCLUDED_FIELDS = List.of(
            "id", "version",
            "createdBy", "createdAt", "createdDttm",
            "updatedBy", "updatedAt", "updatedDttm",
            "deletedBy", "deletedAt", "deletedDttm",
            "certNumber", "assessmentPeriod", "reason", "reasonCode", "reasonDescription");
}
