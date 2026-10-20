package gov.fdic.tip.securityaudit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import gov.fdic.tip.securityaudit.dto.SecurityProfileReportDto;
import gov.fdic.tip.securityaudit.entity.AppUser;
import gov.fdic.tip.securityaudit.repository.AppUserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityProfileReportService {

    private final AppUserRepository appUserRepository;

    public Page<SecurityProfileReportDto> getSecurityProfileReport(
            String search,
            String type,
            String role,
            String status,
            Pageable pageable
    ) {
        // Build dynamic specification based on UI search filters
        Specification<AppUser> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search filter (matches username or display name)
            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                Predicate usernameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern);
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("displayName")), likePattern);
                predicates.add(criteriaBuilder.or(usernameMatch, nameMatch));
            }

            // 2. Status filter (e.g., Active, Disabled, Training Pending)[cite: 40]
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All")) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), status.toLowerCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<AppUser> userPage = appUserRepository.findAll(spec, pageable);

        List<SecurityProfileReportDto> reportDtos = userPage.getContent().stream()
                .map(user -> {
                    String determinedType = determineAccountType(user);
                    String currentRoles = fetchRolesForUser(user.getId());

                    // Filter by Account Type if provided (Human, Service, Unconfirmed)[cite: 40]
                    if (type != null && !type.isBlank() && !type.equalsIgnoreCase("All") && !determinedType.equalsIgnoreCase(type)) {
                        return null; 
                    }

                    // Filter by Role if provided[cite: 40]
                    if (role != null && !role.isBlank() && !role.equalsIgnoreCase("Any role") && !currentRoles.contains(role)) {
                        return null;
                    }

                    return SecurityProfileReportDto.builder()
                            .user(user.getDisplayName() != null ? user.getDisplayName() : user.getLastName() + ", " + user.getFirstName())
                            .userId(user.getUsername())
                            .type(determinedType)
                            .currentRoles(currentRoles)
                            .status(user.getStatus() != null ? user.getStatus() : "Active")
                            .roleStartedOn(user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : null)
                            .lastAccessed(user.getLastLoginAt())
                            .lastTrainingCompleted(user.getTrainingCompletedDate() != null ? user.getTrainingCompletedDate().toLocalDate() : null)
                            .trainingDue(user.getTrainingDueDate() != null ? user.getTrainingDueDate().toLocalDate() : null)
                            .lastModifiedOn(user.getUpdatedAt())
                            .lastModifiedBy(user.getUpdatedBy() != null ? user.getUpdatedBy() : user.getCreatedBy())
                            .build();
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return new PageImpl<>(reportDtos, pageable, userPage.getTotalElements());
    }

    private String determineAccountType(AppUser user) {
        if (user.getEntraId() != null && !user.getEntraId().isBlank()) {
            return "Human";
        } else if (user.getUsername() != null && user.getUsername().toUpperCase().contains("SVC")) {
            return "Service";
        }
        return "Unconfirmed";
    }

    private String fetchRolesForUser(Long userId) {
        // Mock or integrate repository call to fetch actual roles from user_role/app_role tables[cite: 41]
        return "ASA"; 
    }
}