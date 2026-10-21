package gov.fdic.tip.securityaudit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fdic.tip.securityaudit.dto.UserTransactionLogDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for handling User Transaction Logs backed by JaVers audit tables
 * (jv_snapshot, jv_global_id, jv_commit).
 * 
 * Provides dynamic filtering, pagination, JSON state parsing, versioning, and change tracking
 * (INITIAL vs UPDATE states, changed properties, reason codes, descriptions, and metadata).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTransactionLogService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves a paginated list of user transaction logs based on dynamic search criteria.
     *
     * @param user             the user search term (matches local_id or type_name)
     * @param fieldName        specific property changed
     * @param transactionType  INITIAL or UPDATE
     * @param changedFrom      start timestamp filter
     * @param changedTo        end timestamp filter
     * @param certNum          certificate number filter
     * @param assessmentPeriod assessment period filter
     * @param pageable         pagination and sorting information
     * @return a page of UserTransactionLogDto objects
     */
    public Page<UserTransactionLogDto> getUserTransactions(
            String user,
            String fieldName,
            String transactionType,
            LocalDateTime changedFrom,
            LocalDateTime changedTo,
            String certNum,
            String assessmentPeriod,
            Pageable pageable
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.snapshot_pk, s.type, s.version, s.state, s.changed_properties, " +
                "g.type_name, g.local_id, g.global_id_pk, c.commit_date " +
                "FROM jv_snapshot s " +
                "JOIN jv_global_id g ON s.global_id_fk = g.global_id_pk " +
                "LEFT JOIN jv_commit c ON s.commit_fk = c.commit_pk WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        // 1. Filter by User / Record ID / Type Name
        if (user != null && !user.isBlank()) {
            sql.append(" AND (g.local_id ILIKE ? OR g.type_name ILIKE ?)");
            params.add("%" + user.trim() + "%");
            params.add("%" + user.trim() + "%");
        }

        // 2. Filter by Transaction Type (INITIAL / UPDATE)
        if (transactionType != null && !transactionType.isBlank() && !transactionType.equalsIgnoreCase("All")) {
            sql.append(" AND s.type = ?");
            params.add(transactionType.toUpperCase());
        }

        // 3. Filter by Date Range (Changed From / Changed To)
        if (changedFrom != null) {
            sql.append(" AND c.commit_date >= ?");
            params.add(Timestamp.valueOf(changedFrom));
        }
        if (changedTo != null) {
            sql.append(" AND c.commit_date <= ?");
            params.add(Timestamp.valueOf(changedTo));
        }

        // Construct count query for pagination
        String countSql = "SELECT COUNT(*) FROM jv_snapshot s " +
                          "JOIN jv_global_id g ON s.global_id_fk = g.global_id_pk " +
                          "LEFT JOIN jv_commit c ON s.commit_fk = c.commit_pk WHERE 1=1";
        
        StringBuilder countWhere = new StringBuilder();
        List<Object> countParams = new ArrayList<>(params);
        
        if (user != null && !user.isBlank()) {
            countWhere.append(" AND (g.local_id ILIKE ? OR g.type_name ILIKE ?)");
        }
        if (transactionType != null && !transactionType.isBlank() && !transactionType.equalsIgnoreCase("All")) {
            countWhere.append(" AND s.type = ?");
        }
        if (changedFrom != null) {
            countWhere.append(" AND c.commit_date >= ?");
        }
        if (changedTo != null) {
            countWhere.append(" AND c.commit_date <= ?");
        }

        Long total = jdbcTemplate.queryForObject(countSql + countWhere, Long.class, countParams.toArray());
        if (total == null) {
            total = 0L;
        }

        // Append pagination limits to main query
        sql.append(" ORDER BY s.snapshot_pk DESC LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add((int) pageable.getOffset());

        List<UserTransactionLogDto> transactions = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Long snapshotPk = rs.getLong("snapshot_pk");
            String type = rs.getString("type");
            Long version = rs.getLong("version");
            String stateJson = rs.getString("state");
            String changedProps = rs.getString("changed_properties");
            String typeName = rs.getString("type_name");
            String localId = rs.getString("local_id");
            Long globalIdPk = rs.getLong("global_id_pk");
            Timestamp commitDate = rs.getTimestamp("commit_date");

            Map<String, Object> currentState = parseJson(stateJson);
            Map<String, Object> previousState = new HashMap<>();

            // Fetch previous version state if this is an update record
            if ("UPDATE".equalsIgnoreCase(type) && version > 1) {
                previousState = fetchPreviousState(globalIdPk, version);
            }

            // Extract ingested metadata fields from JSON state map
            String certNumVal = currentState.get("certNum") != null ? String.valueOf(currentState.get("certNum")) : null;
            String assessmentPeriodVal = currentState.get("assessmentPeriod") != null ? String.valueOf(currentState.get("assessmentPeriod")) : null;
            String reasonCodeVal = currentState.get("transactionReasonCode") != null ? String.valueOf(currentState.get("transactionReasonCode")) :
                                   (currentState.get("reasonCode") != null ? String.valueOf(currentState.get("reasonCode")) : null);
            String descriptionVal = currentState.get("transactionDescription") != null ? String.valueOf(currentState.get("transactionDescription")) :
                                    (currentState.get("description") != null ? String.valueOf(currentState.get("description")) : null);

            // Optional filter check for certNum
            if (certNum != null && !certNum.isBlank() && (certNumVal == null || !certNumVal.contains(certNum))) {
                return null;
            }
            // Optional filter check for assessmentPeriod
            if (assessmentPeriod != null && !assessmentPeriod.isBlank() && (assessmentPeriodVal == null || !assessmentPeriodVal.contains(assessmentPeriod))) {
                return null;
            }
            // Optional filter check for fieldName
            if (fieldName != null && !fieldName.isBlank()) {
                boolean matchesField = (changedProps != null && changedProps.contains(fieldName)) || currentState.containsKey(fieldName);
                if (!matchesField) {
                    return null;
                }
            }

            return UserTransactionLogDto.builder()
                    .snapshotPk(snapshotPk)
                    .type(type)
                    .version(version)
                    .recordType(typeName)
                    .recordId(localId)
                    .transactionExecuted(commitDate != null ? commitDate.toLocalDateTime().toString() : null)
                    .changedProperties(changedProps)
                    .certNum(certNumVal)
                    .assessmentPeriod(assessmentPeriodVal)
                    .transactionReasonCode(reasonCodeVal)
                    .transactionDescription(descriptionVal)
                    .previousValues(previousState)
                    .currentValue(currentState)
                    .build();
        });

        // Filter out nulls resulting from post-query parameter matching
        List<UserTransactionLogDto> filteredList = transactions.stream()
                .filter(t -> t != null)
                .collect(Collectors.toList());

        return new PageImpl<>(filteredList, pageable, total);
    }

    /**
     * Fetches the state of the immediate predecessor version (version - 1) for a given global ID.
     */
    private Map<String, Object> fetchPreviousState(Long globalIdPk, Long currentVersion) {
        try {
            String sql = "SELECT state FROM jv_snapshot WHERE global_id_fk = ? AND version = ?";
            String prevStateJson = jdbcTemplate.queryForObject(sql, new Object[]{globalIdPk, currentVersion - 1}, String.class);
            return parseJson(prevStateJson);
        } catch (Exception e) {
            log.warn("Could not fetch previous state for globalIdPk: {} at version: {}", globalIdPk, currentVersion - 1);
            return new HashMap<>();
        }
    }

    /**
     * Helper method to parse a JSON string into a Java Map.
     */
    private Map<String, Object> parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse JSON state: {}", jsonStr, e);
            return new HashMap<>();
        }
    }
}