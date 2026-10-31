package gov.fdic.tip.securityaudit.service;

import gov.fdic.tip.securityaudit.dto.SecurityAlertDto;
import gov.fdic.tip.securityaudit.entity.SecurityAlert;
import gov.fdic.tip.securityaudit.repository.SecurityAlertRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityAlertService {

    private final SecurityAlertRepository alertRepository;

    public Page<SecurityAlertDto> getAlerts(
            String search, String condition, String severity, String status,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        Specification<SecurityAlert> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("accountSource")), likePattern),
                    cb.like(cb.lower(root.get("conditionName")), likePattern)
                ));
            }

            if (condition != null && !condition.isBlank() && !condition.equalsIgnoreCase("All")) {
                predicates.add(cb.equal(root.get("conditionName"), condition));
            }

            if (severity != null && !severity.isBlank() && !severity.equalsIgnoreCase("All")) {
                predicates.add(cb.equal(root.get("severity"), SecurityAlert.Severity.valueOf(severity.toUpperCase())));
            }

            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All")) {
                predicates.add(cb.equal(root.get("status"), SecurityAlert.AlertStatus.valueOf(status.toUpperCase())));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("firstDetected"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("firstDetected"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return alertRepository.findAll(spec, pageable).map(SecurityAlertDto::from);
    }

    public SecurityAlertDto getAlertDetail(Long id) {
        SecurityAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Security Alert not found with ID: " + id));
        return SecurityAlertDto.from(alert);
    }

    @Transactional
    public SecurityAlertDto triageAlert(Long id, SecurityAlert.AlertStatus newStatus, 
                                        SecurityAlert.Severity newSeverity, 
                                        String resolutionNote, String username) {
        SecurityAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Security Alert not found with ID: " + id));

        if (newStatus != null) {
            alert.setStatus(newStatus);
        }
        if (newSeverity != null) {
            alert.setSeverity(newSeverity);
        }
        if (resolutionNote != null) {
            alert.setResolutionNote(resolutionNote);
        }
        alert.setHandledBy(username);

        SecurityAlert saved = alertRepository.save(alert);
        return SecurityAlertDto.from(saved);
    }
}