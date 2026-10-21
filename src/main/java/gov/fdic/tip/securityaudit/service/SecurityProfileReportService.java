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
        Specification<AppUser> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search filter (username or display name)
            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                Predicate usernameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern);
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("displayName")), likePattern);
                predicates.add(criteriaBuilder.or(usernameMatch, nameMatch));
            }

            // 2. Status filter
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

                    // Filter by Account Type if provided (Human, Service, Unconfirmed)
                    if (type != null && !type.isBlank() && !type.equalsIgnoreCase("All") && !determinedType.equalsIgnoreCase(type)) {
                        return null; 
                    }

                    // Filter by Role if provided
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

    /**
     * Dynamically fetches assigned role codes (e.g., "ASA", "SYS") for a given user ID 
     * by joining user_role and app_role tables.
     */
    private String fetchRolesForUser(Long userId) {
        String sql = "SELECT r.code FROM app_role r JOIN user_role ur ON r.id = ur.role_id WHERE ur.user_id = ?";
        List<String> roles = jdbcTemplate.queryForList(sql, new Object[]{userId}, String.class);
        
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        // Returns comma-separated roles if a user holds multiple roles (e.g., "ASA, SYS")
        return String.join(", ", roles);
    }
}