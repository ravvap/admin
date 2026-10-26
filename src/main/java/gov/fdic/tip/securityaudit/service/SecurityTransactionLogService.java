package gov.fdic.tip.securityaudit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fdic.tip.securityaudit.dto.SecurityTransactionGridDto;
import gov.fdic.tip.securityaudit.dto.SecurityTransactionLogDto;
import gov.fdic.tip.securityaudit.entity.SecurityTransactionLog;
import gov.fdic.tip.securityaudit.repository.SecurityTransactionLogRepository;
import gov.fdic.tip.securityaudit.repository.SecurityTransactionLogRow;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityTransactionLogService {

    private static final String ATTRIBUTE_STATUS = "Status";

    private final SecurityTransactionLogRepository repository;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // Security Transaction Log grid (native SQL, app_user join, LAG for previous value)
    // ------------------------------------------------------------------
    public Page<SecurityTransactionGridDto> getSecurityTransactionGrid(
            LocalDate fromDate,
            LocalDate toDate,
            String affectedUser,
            String changedBy,
            String transactionType,
            Pageable pageable
    ) {
        List<SecurityTransactionLogRow> rows = repository.findSecurityTransactionGrid(
                fromDate,
                toDate,
                blankToNull(affectedUser),
                blankToNull(changedBy),
                upperOrNull(transactionType),
                pageable.getPageSize(),
                pageable.getOffset()
        );

        long total = rows.isEmpty() ? 0L : rows.get(0).getTotalCount();

        List<SecurityTransactionGridDto> content = rows.stream()
                .map(this::mapGridRow)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, total);
    }

    private SecurityTransactionGridDto mapGridRow(SecurityTransactionLogRow row) {
        String previousValue = row.getPreviousValue();
        String newValue = row.getNewValue();

        // Status events: prefer explicit previousStatus/newStatus when details is JSON.
        if (ATTRIBUTE_STATUS.equals(row.getAttributeChanged()) && row.getDetails() != null) {
            try {
                JsonNode node = objectMapper.readTree(row.getDetails());
                if (node.hasNonNull("previousStatus")) {
                    previousValue = formatStatus(node.get("previousStatus").asText());
                }
                if (node.hasNonNull("newStatus")) {
                    newValue = formatStatus(node.get("newStatus").asText());
                }
            } catch (Exception e) {
                // details is the flat "{key: value}" text, already parsed in SQL
                log.debug("Security log details is not JSON; using SQL-parsed values");
            }
        }

        return SecurityTransactionGridDto.builder()
                .affectedUser(row.getAffectedUser())
                .userId(row.getUserName())
                .modifiedOn(row.getModifiedOn())
                .modifiedBy(row.getModifiedBy())
                .transactionType(row.getTransactionType())
                .attributeChanged(row.getAttributeChanged())
                .previousValue(previousValue)
                .newValue(newValue)
                .build();
    }

    /** training_expired / pending_Training -> "Training Expired" / "Pending Training". */
    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "—";
        }
        String[] parts = status.trim().replace('_', ' ').toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String upperOrNull(String value) {
        String v = blankToNull(value);
        return v == null || "ALL".equalsIgnoreCase(v) ? null : v.toUpperCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------
    // Existing raw-log query (unchanged)
    // ------------------------------------------------------------------
    public Page<SecurityTransactionLogDto> getSecurityTransactionLogs(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String eventType,
            String actorId,
            String outcome,
            String module,
            Pageable pageable
    ) {
        Specification<SecurityTransactionLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("timestamp"), startDate, endDate));
            }

            if (eventType != null && !eventType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), eventType));
            }

            if (actorId != null && !actorId.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("actorId")), "%" + actorId.toLowerCase() + "%"));
            }

            if (outcome != null && !outcome.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("outcome")), outcome.toLowerCase()));
            }

            if (module != null && !module.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("module")), module.toLowerCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<SecurityTransactionLog> entityPage = repository.findAll(spec, pageable);

        List<SecurityTransactionLogDto> dtoList = entityPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }

    private SecurityTransactionLogDto mapToDto(SecurityTransactionLog entity) {
        String reason = "-";
        String message = "-";
        String newStatus = "-";
        String previousStatus = "-";

        if (entity.getDetails() != null) {
            try {
                JsonNode jsonNode = objectMapper.readTree(entity.getDetails());
                if (jsonNode.has("reason")) reason = jsonNode.get("reason").asText();
                if (jsonNode.has("message")) message = jsonNode.get("message").asText();
                if (jsonNode.has("newStatus")) newStatus = jsonNode.get("newStatus").asText();
                if (jsonNode.has("previousStatus")) previousStatus = jsonNode.get("previousStatus").asText();
            } catch (Exception e) {
                log.warn("Failed to parse security log details JSON for id: {}", entity.getId(), e);
            }
        }

        return SecurityTransactionLogDto.builder()
                .id(entity.getId())
                .timestamp(entity.getTimestamp())
                .eventType(entity.getEventType())
                .module(entity.getModule())
                .targetEntityType(entity.getTargetEntityType())
                .targetEntityLabel(entity.getTargetEntityLabel())
                .actorType(entity.getActorType())
                .actorId(entity.getActorId())
                .actorLabel(entity.getActorLabel())
                .outcome(entity.getOutcome())
                .sourceIp(entity.getSourceIp())
                .reason(reason)
                .message(message)
                .newStatus(newStatus)
                .previousStatus(previousStatus)
                .build();
    }
}
