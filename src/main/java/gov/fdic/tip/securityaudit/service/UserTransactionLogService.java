package gov.fdic.tip.securityaudit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fdic.tip.securityaudit.dto.UserTransactionLogDto;
import gov.fdic.tip.securityaudit.entity.JvCommit;
import gov.fdic.tip.securityaudit.entity.JvGlobalId;
import gov.fdic.tip.securityaudit.entity.JvSnapshot;
import gov.fdic.tip.securityaudit.repository.JvCommitRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTransactionLogService {

    private final JvSnapshotRepository snapshotRepository;
    private final JvGlobalIdRepository globalIdRepository;
    private final JvCommitRepository commitRepository; // Optional if joining commit
    private final ObjectMapper objectMapper;

    /**
     * Retrieves a paginated list of user transaction logs based on dynamic search criteria.
     *
     * @param user             the user search term (matches local_id, type_name, or state json)
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
        // Pre-fetch matching global IDs if a user search term is provided
        List<Long> matchingGlobalIdPks = null;
        if (user != null && !user.isBlank()) {
            String pattern = user.trim().toLowerCase();
            matchingGlobalIdPks = globalIdRepository.findAll().stream()
                    .filter(g -> (g.getLocalId() != null && String.valueOf(g.getLocalId()).toLowerCase().contains(pattern)) ||
                                 (g.getTypeName() != null && g.getTypeName().toLowerCase().contains(pattern)))
                    .map(JvGlobalId::getGlobalIdPk)
                    .collect(Collectors.toList());
        }

        final List<Long> globalIdPksFilter = matchingGlobalIdPks;

        // Build JPA Specification including transaction type, user search, and commit date filters
        Specification<JvSnapshot> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join with jv_commit to enable date range filtering
            Join<JvSnapshot, JvCommit> commitJoin = root.join("commit", JoinType.LEFT);

            if (transactionType != null && !transactionType.isBlank() && !transactionType.equalsIgnoreCase("All")) {
                predicates.add(cb.equal(cb.upper(root.get("type")), transactionType.toUpperCase()));
            }

            // Date Range Filters (changedFrom / changedTo)
            if (changedFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(commitJoin.get("commitDate"), changedFrom));
            }
            if (changedTo != null) {
                predicates.add(cb.lessThanOrEqualTo(commitJoin.get("commitDate"), changedTo));
            }

            // User Search Filter
            if (user != null && !user.isBlank()) {
                String pattern = "%" + user.trim().toLowerCase() + "%";
                Predicate stateMatch = cb.like(cb.lower(root.get("state")), pattern);
                
                if (globalIdPksFilter != null && !globalIdPksFilter.isEmpty()) {
                    Predicate globalIdMatch = root.get("globalIdFk").in(globalIdPksFilter);
                    predicates.add(cb.or(globalIdMatch, stateMatch));
                } else {
                    predicates.add(stateMatch);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<JvSnapshot> snapshotPage = snapshotRepository.findAll(spec, pageable);

        // Batch fetch related global IDs to optimize performance
        Set<Long> globalIdFks = snapshotPage.getContent().stream()
                .map(JvSnapshot::getGlobalIdFk)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, JvGlobalId> globalIdMap = globalIdRepository.findAllById(globalIdFks).stream()
                .collect(Collectors.toMap(JvGlobalId::getGlobalIdPk, g -> g));

        List<UserTransactionLogDto> dtos = new ArrayList<>();

        for (JvSnapshot snapshot : snapshotPage.getContent()) {
            Map<String, Object> currentState = parseJson(snapshot.getState());
            Map<String, Object> previousState = new HashMap<>();

            if ("UPDATE".equalsIgnoreCase(snapshot.getType()) && snapshot.getVersion() > 1) {
                snapshotRepository.findByGlobalIdFkAndVersion(snapshot.getGlobalIdFk(), snapshot.getVersion() - 1)
                        .ifPresent(prev -> previousState.putAll(parseJson(prev.getState())));
            }

            JvGlobalId globalId = globalIdMap.get(snapshot.getGlobalIdFk());

            String certNumVal = currentState.get("certNum") != null ? String.valueOf(currentState.get("certNum")) : null;
            String assessmentPeriodVal = currentState.get("assessmentPeriod") != null ? String.valueOf(currentState.get("assessmentPeriod")) : null;
            String reasonCodeVal = currentState.get("transactionReasonCode") != null ? String.valueOf(currentState.get("transactionReasonCode")) : null;
            String descriptionVal = currentState.get("transactionDescription") != null ? String.valueOf(currentState.get("transactionDescription")) : null;

            if (certNum != null && !certNum.isBlank() && (certNumVal == null || !certNumVal.contains(certNum))) {
                continue;
            }
            if (assessmentPeriod != null && !assessmentPeriod.isBlank() && (assessmentPeriodVal == null || !assessmentPeriodVal.contains(assessmentPeriod))) {
                continue;
            }

            // Get execution timestamp from the joined commit entity
            String commitTimestamp = (snapshot.getCommit() != null && snapshot.getCommit().getCommitDate() != null) 
                    ? snapshot.getCommit().getCommitDate().toString() : null;

            UserTransactionLogDto dto = UserTransactionLogDto.builder()
                    .snapshotPk(snapshot.getSnapshotPk())
                    .type(snapshot.getType())
                    .version(snapshot.getVersion())
                    .recordType(globalId != null ? globalId.getTypeName() : null)
                    .recordId(globalId != null ? String.valueOf(globalId.getLocalId()) : null)
                    .transactionExecuted(commitTimestamp)
                    .changedProperties(snapshot.getChangedProperties())
                    .certNum(certNumVal)
                    .assessmentPeriod(assessmentPeriodVal)
                    .transactionReasonCode(reasonCodeVal)
                    .transactionDescription(descriptionVal)
                    .previousValues(previousState)
                    .currentValue(currentState)
                    .build();

            dtos.add(dto);
        }

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