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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTransactionLogService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Page<UserTransactionLogDto> getUserTransactions(
            String user,
            String fieldName,
            String transactionType,
            LocalDate changedFrom,
            LocalDate changedTo,
            String certNum,
            String assessmentPeriod,
            Pageable pageable
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.snapshot_pk, s.type, s.version, s.state, s.changed_properties, " +
                "g.type_name, g.local_id, c.commit_date " +
                "FROM jv_snapshot s " +
                "JOIN jv_global_id g ON s.global_id_fk = g.global_id_pk " +
                "LEFT JOIN jv_commit c ON s.commit_fk = c.commit_pk WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        // 1. Transaction Type Filter
        if (transactionType != null && !transactionType.isBlank() && !transactionType.equalsIgnoreCase("All")) {
            sql.append(" AND s.type = ?");
            params.add(transactionType.toUpperCase());
        }

        // 2. Date Range Filters (MM/dd/yyyy input converted to LocalDateTime boundaries)
        if (changedFrom != null) {
            sql.append(" AND c.commit_date >= ?");
            params.add(Timestamp.valueOf(changedFrom.atStartOfDay()));
        }
        if (changedTo != null) {
            sql.append(" AND c.commit_date <= ?");
            params.add(Timestamp.valueOf(changedTo.atTime(23, 59, 59)));
        }

        // Fetch matching rows
        List<UserTransactionLogDto> allRecords = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Long snapshotPk = rs.getLong("snapshot_pk");
            String type = rs.getString("type");
            Long version = rs.getLong("version");
            String stateJson = rs.getString("state");
            String changedProps = rs.getString("changed_properties");
            String typeName = rs.getString("type_name");
            String localId = rs.getString("local_id");
            Timestamp commitDate = rs.getTimestamp("commit_date");

            Map<String, Object> currentState = parseJson(stateJson);
            Map<String, Object> previousState = new HashMap<>();

            // Fetch previous state for updates if version > 1
            if ("UPDATE".equalsIgnoreCase(type) && version > 1) {
                previousState = fetchPreviousState(localId, typeName, version - 1);
            }

            // Extract metadata from JSON state
            String certNumVal = currentState.get("certNum") != null ? String.valueOf(currentState.get("certNum")) : null;
            String assessmentPeriodVal = currentState.get("assessmentPeriod") != null ? String.valueOf(currentState.get("assessmentPeriod")) : null;
            String reasonCodeVal = currentState.get("transactionReasonCode") != null ? String.valueOf(currentState.get("transactionReasonCode")) :
                                   (currentState.get("reasonCode") != null ? String.valueOf(currentState.get("reasonCode")) : null);
            String descriptionVal = currentState.get("transactionDescription") != null ? String.valueOf(currentState.get("transactionDescription")) :
                                    (currentState.get("description") != null ? String.valueOf(currentState.get("description")) : null);
            String createdByVal = currentState.get("createdBy") != null ? String.valueOf(currentState.get("createdBy")) : null;
            String updatedByVal = currentState.get("updatedBy") != null ? String.valueOf(currentState.get("updatedBy")) : null;

            // 3. User / Creator / Updater Search Filter
            if (user != null && !user.isBlank()) {
                String searchKeyword = user.trim().toLowerCase();
                boolean matchesRecordId = (localId != null && localId.toLowerCase().contains(searchKeyword));
                boolean matchesTypeName = (typeName != null && typeName.toLowerCase().contains(searchKeyword));
                boolean matchesCreator = (createdByVal != null && createdByVal.toLowerCase().contains(searchKeyword));
                boolean matchesUpdater = (updatedByVal != null && updatedByVal.toLowerCase().contains(searchKeyword));

                if (!matchesRecordId && !matchesTypeName && !matchesCreator && !matchesUpdater) {
                    return null;
                }
            }

            // 4. Optional Certificate & Assessment Period Filters
            if (certNum != null && !certNum.isBlank() && (certNumVal == null || !certNumVal.contains(certNum))) {
                return null;
            }
            if (assessmentPeriod != null && !assessmentPeriod.isBlank() && (assessmentPeriodVal == null || !assessmentPeriodVal.contains(assessmentPeriod))) {
                return null;
            }

            // 5. Optional Field Name Filter
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

        // Filter out nulls from criteria matching
        List<UserTransactionLogDto> filteredList = allRecords.stream()
                .filter(r -> r != null)
                .collect(Collectors.toList());

        // Apply Manual Pagination on the filtered list
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());
        
        List<UserTransactionLogDto> pageContent = start > filteredList.size() ? new ArrayList<>() : filteredList.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredList.size());
    }

    private Map<String, Object> fetchPreviousState(String localId, String typeName, Long targetVersion) {
        try {
            String sql = "SELECT s.state FROM jv_snapshot s " +
                         "JOIN jv_global_id g ON s.global_id_fk = g.global_id_pk " +
                         "WHERE g.local_id = ? AND g.type_name = ? AND s.version = ?";
            String stateJson = jdbcTemplate.queryForObject(sql, new Object[]{localId, typeName, targetVersion}, String.class);
            return parseJson(stateJson);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<String, Object> parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}