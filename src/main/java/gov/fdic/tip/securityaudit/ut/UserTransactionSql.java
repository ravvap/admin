package gov.tip.admin.securityaudit.constants;

/**
 * SQL over the Javers tables (PostgreSQL). One row of the "txn" CTE = one field of one snapshot:
 * <ul>
 *   <li>user / date: jv_commit.author and commit date (UTC instant, shown in ET)</li>
 *   <li>transaction type: commit property "transactionType" if present, otherwise derived from the snapshot
 *       type (INITIAL = CREATE, UPDATE = UPDATE, TERMINAL = DELETE)</li>
 *   <li>field: each name in jv_snapshot.changed_properties (for DELETE: every property of the last state)</li>
 *   <li>current value: that property in this snapshot's state JSON (null for DELETE)</li>
 *   <li>previous value: the same property in the previous snapshot (version - 1); null for creates</li>
 *   <li>cert / period / reason code / description: commit properties first, then the state JSON,
 *       otherwise "Not applicable"</li>
 * </ul>
 */
public final class UserTransactionSql {

    private UserTransactionSql() {
    }

    public static final String ENTITY_FILTER_PLACEHOLDER = "/*ENTITY_FILTER*/";
    public static final String ENTITY_FILTER = "AND g.type_name IN (:entityTypes)";

    public static final String TXN_CTE = """
            WITH txn AS (
                SELECT s.snapshot_pk                                              AS snapshot_pk,
                       c.author                                                   AS user_id,
                       COALESCE(c.commit_date_instant::timestamptz,
                                c.commit_date AT TIME ZONE 'UTC')                 AS changed_at,
                       COALESCE(NULLIF(cp_cert.property_value, ''),
                                NULLIF(ctx.state ->> 'certNumber', ''),
                                :notApplicable)                                   AS cert_number,
                       COALESCE(NULLIF(cp_period.property_value, ''),
                                NULLIF(ctx.state ->> 'assessmentPeriod', ''),
                                :notApplicable)                                   AS assessment_period,
                       prop.field_name                                            AS field_name,
                       UPPER(COALESCE(NULLIF(cp_type.property_value, ''),
                                      CASE s.type WHEN 'INITIAL'  THEN 'CREATE'
                                                  WHEN 'TERMINAL' THEN 'DELETE'
                                                  ELSE 'UPDATE' END))             AS transaction_type,
                       COALESCE(NULLIF(cp_code.property_value, ''),
                                NULLIF(ctx.state ->> 'reasonCode', ''),
                                :notApplicable)                                   AS reason_code,
                       COALESCE(NULLIF(cp_desc.property_value, ''),
                                NULLIF(ctx.state ->> 'reasonDescription', ''),
                                NULLIF(ctx.state ->> 'reason', ''),
                                :notApplicable)                                   AS reason_description,
                       LEFT(prev.state ->> prop.field_name, :maxValueLength)      AS previous_value,
                       CASE WHEN s.type = 'TERMINAL' THEN NULL
                            ELSE LEFT(st.state ->> prop.field_name, :maxValueLength) END AS current_value
                FROM jv_snapshot s
                JOIN jv_global_id g ON g.global_id_pk = s.global_id_fk
                JOIN jv_commit c ON c.commit_pk = s.commit_fk
                CROSS JOIN LATERAL (SELECT s.state::jsonb AS state) st
                LEFT JOIN jv_snapshot ps ON ps.global_id_fk = s.global_id_fk AND ps.version = s.version - 1
                LEFT JOIN LATERAL (SELECT ps.state::jsonb AS state) prev ON TRUE
                CROSS JOIN LATERAL (SELECT CASE WHEN s.type = 'TERMINAL' THEN prev.state ELSE st.state END AS state) ctx
                CROSS JOIN LATERAL (
                        SELECT jsonb_array_elements_text(
                                   COALESCE(NULLIF(s.changed_properties, '')::jsonb, '[]'::jsonb)) AS field_name
                         WHERE s.type <> 'TERMINAL'
                        UNION ALL
                        SELECT jsonb_object_keys(prev.state)
                         WHERE s.type = 'TERMINAL' AND jsonb_typeof(prev.state) = 'object'
                ) prop
                LEFT JOIN jv_commit_property cp_cert
                       ON cp_cert.commit_fk = c.commit_pk AND cp_cert.property_name = :certProp
                LEFT JOIN jv_commit_property cp_period
                       ON cp_period.commit_fk = c.commit_pk AND cp_period.property_name = :periodProp
                LEFT JOIN jv_commit_property cp_type
                       ON cp_type.commit_fk = c.commit_pk AND cp_type.property_name = :typeProp
                LEFT JOIN jv_commit_property cp_code
                       ON cp_code.commit_fk = c.commit_pk AND cp_code.property_name = :codeProp
                LEFT JOIN jv_commit_property cp_desc
                       ON cp_desc.commit_fk = c.commit_pk AND cp_desc.property_name = :descProp
                WHERE s.type IN ('INITIAL', 'UPDATE', 'TERMINAL')
                  AND prop.field_name NOT IN (:excludedFields)
                  /*ENTITY_FILTER*/
            )
            """;

    public static final String SELECT_PAGE = """
            SELECT snapshot_pk, user_id, changed_at, cert_number, assessment_period, field_name,
                   transaction_type, reason_code, reason_description, previous_value, current_value
            FROM txn
            """;

    public static final String COUNT = "SELECT COUNT(*) FROM txn";

    public static final String DISTINCT_ASSESSMENT_PERIODS =
            "SELECT DISTINCT assessment_period FROM txn WHERE assessment_period <> :notApplicable ORDER BY assessment_period DESC";

    public static final String DISTINCT_FIELD_NAMES =
            "SELECT DISTINCT field_name FROM txn ORDER BY field_name";

    public static final String DISTINCT_TRANSACTION_TYPES =
            "SELECT DISTINCT transaction_type FROM txn ORDER BY transaction_type";
}
