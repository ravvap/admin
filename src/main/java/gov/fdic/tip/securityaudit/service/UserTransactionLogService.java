package gov.fdic.tip.securityaudit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fdic.tip.securityaudit.dto.UserTransactionLogDto;
import gov.fdic.tip.securityaudit.entity.JvCommit;
import gov.fdic.tip.securityaudit.entity.JvGlobalId;
import gov.fdic.tip.securityaudit.entity.JvSnapshot;
import gov.fdic.tip.securityaudit.repository.JvGlobalIdRepository;
import gov.fdic.tip.securityaudit.repository.JvSnapshotRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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

    private final JvSnapshotRepository snapshotRepository;
    private final JvGlobalIdRepository globalIdRepository; // Optional if using join specs
    private final ObjectMapper objectMapper;

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
        Specification<JvSnapshot> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<JvSnapshot, JvGlobalId> globalIdJoin = root.join("globalIdFk", JoinType.LEFT); // Or map entity relationship
            Join<JvSnapshot, JvCommit> commitJoin = root.join("commitFk", JoinType.LEFT);

            if (user != null && !user.isBlank()) {
                String pattern = "%" + user.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(globalIdJoin.get("localId")), pattern),
                        cb.like(cb.lower(globalIdJoin.get("typeName")), pattern),
                        cb.like(cb.lower(root.get("state")), pattern)
                ));
            }

            if (transactionType != null && !transactionType.isBlank() && !transactionType.equalsIgnoreCase("All")) {
                predicates.add(cb.equal(cb.upper(root.get("type")), transactionType.toUpperCase()));
            }

            if (changedFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(commitJoin.get("commitDate"), changedFrom));
            }
            if (changedTo != null) {
                predicates.add(cb.lessThanOrEqualTo(commitJoin.get("commitDate"), changedTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<JvSnapshot> snapshotPage = snapshotRepository.findAll(spec, pageable);

        List<UserTransactionLogDto> dtos = snapshotPage.getContent().stream().map(snapshot -> {
            Map<String, Object> currentState = parseJson(snapshot.getState());
            Map<String, Object> previousState = new HashMap<>();

            if ("UPDATE".equalsIgnoreCase(snapshot.getType()) && snapshot.getVersion() > 1) {
                snapshotRepository.findByGlobalIdFkAndVersion(snapshot.getGlobalIdFk(), snapshot.getVersion() - 1)
                        .ifPresent(prev -> previousState.putAll(parseJson(prev.getState())));
            }

            JvGlobalId globalId = globalIdRepository.findById(snapshot.getGlobalIdFk()).orElse(null);

            String certNumVal = currentState.get("certNum") != null ? String.valueOf(currentState.get("certNum")) : null;
            String assessmentPeriodVal = currentState.get("assessmentPeriod") != null ? String.valueOf(currentState.get("assessmentPeriod")) : null;

            if (certNum != null && !certNum.isBlank() && (certNumVal == null || !certNumVal.contains(certNum))) {
                return null;
            }
            if (assessmentPeriod != null && !assessmentPeriod.isBlank() && (assessmentPeriodVal == null || !assessmentPeriodVal.contains(assessmentPeriod))) {
                return null;
            }

            return UserTransactionLogDto.builder()
                    .snapshotPk(snapshot.getSnapshotPk())
                    .type(snapshot.getType())
                    .version(snapshot.getVersion())
                    .recordType(globalId != null ? globalId.getTypeName() : null)
                    .recordId(globalId != null ? globalId.getLocalId() : null)
                    .changedProperties(snapshot.getChangedProperties())
                    .certNum(certNumVal)
                    .assessmentPeriod(assessmentPeriodVal)
                    .previousValues(previousState)
                    .currentValue(currentState)
                    .build();
        }).filter(dto -> dto != null).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, snapshotPage.getTotalElements());
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