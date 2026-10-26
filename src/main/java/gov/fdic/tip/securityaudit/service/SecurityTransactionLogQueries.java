package gov.fdic.tip.securityaudit.repository;

/**
 * Native SQL for the Security Transaction Log grid.
 * Kept in a constants class (compile-time constant, usable in @Query).
 */
public final class SecurityTransactionLogQueries {

    private SecurityTransactionLogQueries() {
    }

    public static final String GRID_SQL = """
            WITH sec AS (
                SELECT
                    COALESCE(CAST(u.id AS text), b.actor_id)        AS user_key,
                    COALESCE(u.display_name, b.actor_id)            AS affected_user,
                    u.username                                      AS user_name,
                    b.actor_id                                      AS actor_id,
                    b.actor_type                                    AS actor_type,
                    b.details                                       AS details,
                    (b."timestamp" AT TIME ZONE 'America/New_York') AS ts_et,

                    CASE b.event_type
                        WHEN 'AUTH.USER_PROVISIONED_JIT' THEN 'ADD USER'
                        WHEN 'AUTH.ADD_ROLE(S)'          THEN 'ADD ROLE'
                        WHEN 'AUTH.REMOVE_ROLE(S)'       THEN 'REMOVE ROLE'
                        WHEN 'AUTH.ADD_OVERRIDE(S)'      THEN 'ALLOW PAGE'
                        WHEN 'AUTH.DENY_PAGE'            THEN 'DENY PAGE'
                        WHEN 'AUTH.UPDATE_USER'          THEN 'UPDATE USER'
                        WHEN 'AUTH.DISABLE_USER'         THEN 'DISABLE USER'
                    END AS transaction_type,

                    CASE b.event_type
                        WHEN 'AUTH.ADD_ROLE(S)'          THEN 'Role'
                        WHEN 'AUTH.REMOVE_ROLE(S)'       THEN 'Role'
                        WHEN 'AUTH.ADD_OVERRIDE(S)'      THEN 'Allowed Pages'
                        WHEN 'AUTH.DENY_PAGE'            THEN 'Denied Pages'
                        WHEN 'AUTH.UPDATE_USER'          THEN 'Status'
                        WHEN 'AUTH.DISABLE_USER'         THEN 'Status'
                        WHEN 'AUTH.USER_PROVISIONED_JIT' THEN 'Account'
                    END AS attribute_changed,

                    CASE
                        WHEN b.event_type IN ('AUTH.ADD_ROLE(S)', 'AUTH.REMOVE_ROLE(S)')
                            THEN NULLIF(trim(split_part(split_part(b.details, 'roles: [', 2), ']', 1)), '')
                        WHEN b.event_type IN ('AUTH.ADD_OVERRIDE(S)', 'AUTH.DENY_PAGE')
                            THEN NULLIF(trim(split_part(split_part(b.details, 'pageOverrides: [', 2), ']', 1)), '')
                        WHEN b.event_type IN ('AUTH.UPDATE_USER', 'AUTH.DISABLE_USER')
                            THEN NULLIF(initcap(replace(lower(
                                     trim(both ' }' from split_part(split_part(b.details, 'status: ', 2), ',', 1))
                                 ), '_', ' ')), '')
                        WHEN b.event_type = 'AUTH.USER_PROVISIONED_JIT'
                            THEN 'Account created'
                    END AS new_value
                FROM business_events_audit b
                LEFT JOIN app_user u
                       ON lower(u.email_address) = lower(b.actor_id)
                WHERE b.event_type IN (
                        'AUTH.USER_PROVISIONED_JIT', 'AUTH.ADD_ROLE(S)', 'AUTH.REMOVE_ROLE(S)',
                        'AUTH.ADD_OVERRIDE(S)', 'AUTH.DENY_PAGE', 'AUTH.UPDATE_USER', 'AUTH.DISABLE_USER')
                  AND (CAST(:toDate AS date) IS NULL
                       OR (b."timestamp" AT TIME ZONE 'America/New_York') < CAST(:toDate AS date) + 1)
            ),
            grid AS (
                SELECT
                    s.*,
                    LAG(s.new_value) OVER (
                        PARTITION BY s.user_key, s.attribute_changed
                        ORDER BY s.ts_et
                    ) AS previous_value
                FROM sec s
                WHERE NOT (s.attribute_changed IN ('Allowed Pages', 'Denied Pages') AND s.new_value IS NULL)
            )
            SELECT
                g.affected_user                                   AS "affectedUser",
                g.user_name                                       AS "userName",
                TO_CHAR(g.ts_et, 'MM/DD/YYYY HH12:MI AM')         AS "modifiedOn",
                CASE WHEN g.actor_type = 'SYSTEM' THEN 'SYSTEM'
                     ELSE g.affected_user END                     AS "modifiedBy",
                g.transaction_type                                AS "transactionType",
                g.attribute_changed                               AS "attributeChanged",
                COALESCE(g.previous_value, '—')                   AS "previousValue",
                COALESCE(g.new_value, '—')                        AS "newValue",
                g.details                                         AS "details",
                COUNT(*) OVER ()                                  AS "totalCount"
            FROM grid g
            WHERE (CAST(:fromDate AS date) IS NULL OR CAST(g.ts_et AS date) >= CAST(:fromDate AS date))
              AND (CAST(:toDate AS date) IS NULL OR CAST(g.ts_et AS date) <= CAST(:toDate AS date))
              AND (CAST(:affectedUser AS text) IS NULL
                   OR g.affected_user ILIKE '%' || CAST(:affectedUser AS text) || '%'
                   OR g.user_name     ILIKE '%' || CAST(:affectedUser AS text) || '%')
              AND (CAST(:changedBy AS text) IS NULL
                   OR g.actor_id ILIKE '%' || CAST(:changedBy AS text) || '%')
              AND (CAST(:transactionType AS text) IS NULL
                   OR g.transaction_type = CAST(:transactionType AS text))
            ORDER BY g.ts_et DESC
            LIMIT CAST(:pageSize AS integer) OFFSET CAST(:pageOffset AS bigint)
            """;
}
