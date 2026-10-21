package gov.fdic.tip.securityaudit.service;

import gov.fdic.tip.domain.AppUser;
import gov.fdic.tip.securityaudit.dto.SecurityProfileReportDto;
import gov.fdic.tip.securityaudit.repository.AppUserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityProfileReportService {

    private final AppUserRepository appUserRepository;
    private final JdbcTemplate jdbcTemplate;

    public Page<SecurityProfileReportDto> getSecurityProfileReport(
            String search,
            String type,
            String role,
            String status,
            Pageable pageable
    ) {
        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted users if applicable
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Search filter (User ID / Username or Display Name)
            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                Predicate usernameMatch = cb.like(cb.lower(root.get("username")), likePattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("displayName")), likePattern);
                predicates.add(cb.or(usernameMatch, nameMatch));
            }

            // Status filter
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All")) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AppUser> userPage = appUserRepository.findAll(spec, pageable);

        List<SecurityProfileReportDto> dtos = new ArrayList<>();

        for (AppUser user : userPage.getContent()) {
            String determinedType = determineAccountType(user);
            String currentRoles = fetchRolesForUser(user.getId());
            String computedStatus = user.getStatus() != null && !user.getStatus().isBlank() 
                                     ? user.getStatus() 
                                     : (Boolean.TRUE.equals(user.getIsActive()) ? "Active" : "Disabled");

            // Filter by Account Type
            if (type != null && !type.isBlank() && !type.equalsIgnoreCase("All") && !determinedType.equalsIgnoreCase(type)) {
                continue;
            }

            // Filter by Role
            if (role != null && !role.isBlank() && !role.equalsIgnoreCase("Any role") && !currentRoles.toLowerCase().contains(role.toLowerCase())) {
                continue;
            }

            // Correct mapping for Display Name
            String displayName = user.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                if (user.getLastName() != null && user.getFirstName() != null) {
                    displayName = user.getLastName() + ", " + user.getFirstName();
                } else {
                    displayName = user.getUsername();
                }
            }

            SecurityProfileReportDto dto = SecurityProfileReportDto.builder()
                    .user(displayName)
                    .userId(user.getUsername()) // Fix: Uses username (e.g., JPRICE, GPRATT)
                    .type(determinedType)
                    .currentRoles(currentRoles)  // Fix: Fetches assigned role codes from user_role
                    .status(computedStatus)      // Fix: Returns explicit DB status
                    .roleStartedOn(user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : null)
                    .lastAccessed(user.getLastLoginAt())
                    .lastTrainingCompleted(user.getTrainingCompletedDate() != null ? user.getTrainingCompletedDate().toLocalDate() : null)
                    .trainingDue(user.getTrainingDueDate() != null ? user.getTrainingDueDate().toLocalDate() : null)
                    .lastModifiedOn(user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt())
                    .lastModifiedBy(user.getUpdatedBy() != null ? user.getUpdatedBy() : user.getCreatedBy())
                    .build();

            dtos.add(dto);
        }

        return new PageImpl<>(dtos, pageable, userPage.getTotalElements());
    }

    private String determineAccountType(AppUser user) {
        if (user.getUsername() != null && user.getUsername().toUpperCase().startsWith("SVC")) {
            return "Service";
        } else if (user.getEntraId() != null && !user.getEntraId().isBlank()) {
            return "Human";
        }
        return "Unconfirmed";
    }

    /**
     * Directly queries user_role and app_role tables to get roles for a given user ID.
     */
    private String fetchRolesForUser(Long userId) {
        if (userId == null) {
            return "";
        }
        String sql = "SELECT r.code FROM app_role r JOIN user_role ur ON r.id = ur.role_id WHERE ur.user_id = ?";
        List<String> roles = jdbcTemplate.queryForList(sql, String.class, userId);
        
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return String.join(", ", roles);
    }
}