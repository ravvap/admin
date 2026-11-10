package gov.tip.admin.securityaudit.repository;

import static gov.tip.admin.securityaudit.constants.JaversAuditConstants.*;
import static gov.tip.admin.securityaudit.constants.UserTransactionConstants.*;

import gov.tip.admin.securityaudit.config.UserTransactionProperties;
import gov.tip.admin.securityaudit.constants.UserTransactionSql;
import gov.tip.admin.securityaudit.dto.UserTransactionFilter;
import gov.tip.admin.securityaudit.dto.UserTransactionResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only access to the Javers tables. Values are always bound as named parameters; sort columns come
 * from a fixed whitelist, so no user input is ever concatenated into SQL.
 */
@Repository
@RequiredArgsConstructor
public class UserTransactionRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final UserTransactionProperties properties;

    public List<UserTransactionResponse> findAll(UserTransactionFilter filter, Sort sort, int limit, long offset) {
        Map<String, Object> params = baseParams();
        String where = buildWhere(filter, params);
        params.put("limit", limit);
        params.put("offset", offset);

        String sql = cte() + UserTransactionSql.SELECT_PAGE + where + buildOrderBy(sort) + " LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, new MapSqlParameterSource(params), (rs, i) -> map(rs));
    }

    public long count(UserTransactionFilter filter) {
        Map<String, Object> params = baseParams();
        String where = buildWhere(filter, params);
        Long total = jdbc.queryForObject(cte() + UserTransactionSql.COUNT + where, params, Long.class);
        return total == null ? 0 : total;
    }

    public List<String> findDistinctAssessmentPeriods() {
        return jdbc.queryForList(cte() + UserTransactionSql.DISTINCT_ASSESSMENT_PERIODS, baseParams(), String.class);
    }

    public List<String> findDistinctFieldNames() {
        return jdbc.queryForList(cte() + UserTransactionSql.DISTINCT_FIELD_NAMES, baseParams(), String.class);
    }

    public List<String> findDistinctTransactionTypes() {
        return jdbc.queryForList(cte() + UserTransactionSql.DISTINCT_TRANSACTION_TYPES, baseParams(), String.class);
    }

    // ---- SQL building ----

    private String cte() {
        String entityFilter = properties.getIncludedEntityTypes().isEmpty() ? "" : UserTransactionSql.ENTITY_FILTER;
        return UserTransactionSql.TXN_CTE.replace(UserTransactionSql.ENTITY_FILTER_PLACEHOLDER, entityFilter);
    }

    private Map<String, Object> baseParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("maxValueLength", VALUE_MAX);
        params.put("notApplicable", NOT_APPLICABLE);
        params.put("typeProp", COMMIT_PROP_TRANSACTION_TYPE);
        params.put("certProp", COMMIT_PROP_CERT_NUMBER);
        params.put("periodProp", COMMIT_PROP_ASSESSMENT_PERIOD);
        params.put("codeProp", COMMIT_PROP_REASON_CODE);
        params.put("descProp", COMMIT_PROP_REASON_DESCRIPTION);
        params.put("excludedFields", EXCLUDED_FIELDS);
        if (!properties.getIncludedEntityTypes().isEmpty()) {
            params.put("entityTypes", properties.getIncludedEntityTypes());
        }
        return params;
    }

    private String buildWhere(UserTransactionFilter f, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();

        if (hasText(f.user())) {
            clauses.add("user_id ILIKE :user ESCAPE '" + LIKE_ESCAPE + "'");
            params.put("user", "%" + escapeLike(f.user().trim()) + "%");
        }
        if (f.certNumber() != null) {
            clauses.add("cert_number = :certNumber");
            params.put("certNumber", String.valueOf(f.certNumber()));
        }
        if (hasText(f.fieldName())) {
            clauses.add("field_name = :fieldName");
            params.put("fieldName", f.fieldName().trim());
        }
        if (hasText(f.transactionType())) {
            clauses.add("transaction_type = :transactionType");
            params.put("transactionType", f.transactionType().trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(f.assessmentPeriod())) {
            clauses.add("assessment_period = :assessmentPeriod");
            params.put("assessmentPeriod", f.assessmentPeriod().trim());
        }
        // Date range is inclusive of both days, evaluated in Eastern Time (what the screen shows)
        if (f.changedFrom() != null) {
            clauses.add("changed_at >= CAST(:changedFrom AS timestamptz)");
            params.put("changedFrom", f.changedFrom().atStartOfDay(DISPLAY_ZONE).toOffsetDateTime());
        }
        if (f.changedTo() != null) {
            clauses.add("changed_at < CAST(:changedToExclusive AS timestamptz)");
            params.put("changedToExclusive", f.changedTo().plusDays(1).atStartOfDay(DISPLAY_ZONE).toOffsetDateTime());
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private String buildOrderBy(Sort sort) {
        List<String> parts = new ArrayList<>();
        for (Sort.Order order : sort) {
            String column = SORT_COLUMNS.get(order.getProperty());
            if (column != null) {
                parts.add(column + (order.isAscending() ? " ASC" : " DESC") + " NULLS LAST");
            }
        }
        if (parts.isEmpty()) {
            parts.add(SORT_COLUMNS.get(DEFAULT_SORT_FIELD) + " DESC");
        }
        parts.add(SORT_TIE_BREAKER);
        return " ORDER BY " + String.join(", ", parts);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String escapeLike(String s) {
        String e = String.valueOf(LIKE_ESCAPE);
        return s.replace(e, e + e).replace("%", e + "%").replace("_", e + "_");
    }

    // ---- mapping ----

    private static UserTransactionResponse map(ResultSet rs) throws SQLException {
        OffsetDateTime changedAt = rs.getObject("changed_at", OffsetDateTime.class);
        return new UserTransactionResponse(
                rs.getLong("snapshot_pk"),
                rs.getString("user_id"),
                changedAt == null ? null : changedAt.atZoneSameInstant(DISPLAY_ZONE),
                rs.getString("cert_number"),
                rs.getString("assessment_period"),
                rs.getString("field_name"),
                rs.getString("transaction_type"),
                rs.getString("reason_code"),
                rs.getString("reason_description"),
                rs.getString("previous_value"),
                rs.getString("current_value"));
    }
}
