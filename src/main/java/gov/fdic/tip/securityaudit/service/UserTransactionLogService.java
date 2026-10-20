package gov.fdic.tip.securityaudit.service;

import gov.fdic.tip.securityaudit.dto.UserTransactionLogDto;
import gov.fdic.tip.securityaudit.entity.JvSnapshot;
import gov.fdic.tip.securityaudit.repository.JvSnapshotRepository;
import gov.fdic.tip.securityaudit.util.JvSnapshotParserUtil; // Import your util
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTransactionLogService {

    private final JvSnapshotRepository jvSnapshotRepository;
    private final JvSnapshotParserUtil jvSnapshotParserUtil; // Inject the utility component

    public Page<UserTransactionLogDto> getUserTransactionLogs(
            String assessmentPeriodFilter,
            String userSearch,
            String transactionType,
            String managedType,
            Pageable pageable
    ) {
        Specification<JvSnapshot> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (transactionType != null && !transactionType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), transactionType));
            }

            if (managedType != null && !managedType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("managedType"), managedType));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<JvSnapshot> page = jvSnapshotRepository.findAll(spec, pageable);

        List<UserTransactionLogDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private UserTransactionLogDto mapToDto(JvSnapshot snapshot) {
        String createdBy = "-";
        LocalDateTime dateTime = null;
        String reasonCode = "-";
        String description = "-";
        String previousValue = "-";
        String currentValue = "-";
        String certNum = "-";
        String assessmentPeriod = "-";
        String fieldName = snapshot.getChangedProperties() != null ? snapshot.getChangedProperties() : "-";

        // Use the utility to extract values from current state JSON
        if (snapshot.getState() != null) {
            createdBy = jvSnapshotParserUtil.extractFieldFromJson(snapshot.getState(), "createdBy");
            if ("-".equals(createdBy)) {
                createdBy = jvSnapshotParserUtil.extractFieldFromJson(snapshot.getState(), "updatedBy");
            }

            String dateStr = jvSnapshotParserUtil.extractFieldFromJson(snapshot.getState(), "createdAt");
            if (!"-".equals(dateStr)) {
                try {
                    dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    log.warn("Failed to parse date string: {}", dateStr);
                }
            }

            reasonCode = jvSnapshotParserUtil.extractFieldFromJson(snapshot.getState(), "reasonCode");
            description = jvSnapshotParserUtil.extractFieldFromJson(snapshot.getState(), "description");
            certNum = jvSnapshotParserUtil.extractFieldFromJson(snapshot.getState(), "certNumber");
            assessmentPeriod = jvSnapshotParserUtil.extractFieldFromJson(snapshot.getState(), "assessmentPeriod");

            // Extract Current Value for the changed property
            if (snapshot.getChangedProperties() != null) {
                currentValue = jvSnapshotParserUtil.extractFieldFromJson(snapshot.getState(), snapshot.getChangedProperties());
            }
        }

        // Determine Previous Value based on transaction type (INITIAL vs UPDATE)
        if ("INITIAL".equalsIgnoreCase(snapshot.getType())) {
            previousValue = "-";
        } else {
            // Fetch previous version snapshot for the same global entity ID
            JvSnapshot previousSnapshot = jvSnapshotRepository
                    .findAll((root, query, cb) -> cb.and(
                            cb.equal(root.get("globalIdFk"), snapshot.getGlobalIdFk()),
                            cb.equal(root.get("version"), snapshot.getVersion() - 1)
                    ))
                    .stream().findFirst().orElse(null);

            if (previousSnapshot != null && snapshot.getChangedProperties() != null) {
                previousValue = jvSnapshotParserUtil.extractFieldFromJson(previousSnapshot.getState(), snapshot.getChangedProperties());
            }
        }

        return UserTransactionLogDto.builder()
                .snapshotPk(snapshot.getSnapshotPk())
                .user(createdBy)
                .dateTime(dateTime)
                .certNumber(certNum)
                .assessmentPeriod(assessmentPeriod)
                .field(fieldName)
                .transactionType(snapshot.getType())
                .transactionReasonCode(reasonCode)
                .transactionDescription(description)
                .previousValue(previousValue)
                .currentValue(currentValue)
                .build();
    }
}