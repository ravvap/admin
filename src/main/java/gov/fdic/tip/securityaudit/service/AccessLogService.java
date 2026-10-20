package gov.fdic.tip.securityaudit.service;

import gov.fdic.tip.securityaudit.entity.AccessLog;
import gov.fdic.tip.securityaudit.repository.AccessLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    public Page<AccessLog> getAccessLogs(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String userId,
            AccessLog.AccountType accountType,
            Boolean hideServiceAccounts,
            Boolean loginSuccessful,
            AccessLog.AccessType accessType,
            Pageable pageable
    ) {
        Specification<AccessLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Date Range filter (Quarterly default support)
            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("loginDateTime"), startDate, endDate));
            }

            // User ID filter
            if (userId != null && !userId.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("userId")), "%" + userId.toLowerCase() + "%"));
            }

            // Specific account type filter
            if (accountType != null) {
                predicates.add(criteriaBuilder.equal(root.get("accountType"), accountType));
            }

            // Hide Service Accounts toggle (US-AL-3)
            if (Boolean.TRUE.equals(hideServiceAccounts)) {
                predicates.add(criteriaBuilder.notEqual(root.get("accountType"), AccessLog.AccountType.SERVICE));
            }

            // Login Success/Failure filter (US-AL-4)
            if (loginSuccessful != null) {
                predicates.add(criteriaBuilder.equal(root.get("loginSuccessful"), loginSuccessful));
            }

            // Access Type filter (Online / SQL Access)
            if (accessType != null) {
                predicates.add(criteriaBuilder.equal(root.get("accessType"), accessType));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return accessLogRepository.findAll(spec, pageable);
    }

    
}