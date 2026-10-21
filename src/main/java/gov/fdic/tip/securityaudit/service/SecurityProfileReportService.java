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
        StringBuilder sql = new StringBuilder(
            "SELECT u.display_name, u.first_name, u.last_name, u.username, u.entra_id, u.status, " +
            "u.created_at, u.last_login_at, u.training_completed_date, u.training_due_date, " +
            "u.updated_at, u.created_by, u.updated_by, " +
            "STRING_AGG(r.code, ', ') AS current_roles " +
            "FROM app_user u " +
            "LEFT JOIN user_role ur ON u.id = ur.user_id " +
            "LEFT JOIN app_role r ON ur.role_id = r.id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        // 1. Search Filter (Username or Display Name)
        if (search != null && !search.isBlank()) {
            sql.append(" AND (u.username ILIKE ? OR u.display_name ILIKE ? OR u.first_name ILIKE ? OR u.last_name ILIKE ?) ");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        // 2. Status Filter
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All")) {
            sql.append(" AND u.status ILIKE ? ");
            params.add(status.trim());
        }

        sql.append(" GROUP BY u.id ");

        // 3. Role Filter (HAVING clause for aggregated roles)
        if (role != null && !role.isBlank() && !role.equalsIgnoreCase("Any role")) {
            sql.append(" HAVING STRING_AGG(r.code, ', ') ILIKE ? ");
            params.add("%" + role.trim() + "%");
        }

        // Total count query for pagination
        String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ") AS filtered_count";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        if (total == null) {
            total = 0L;
        }

        // Add Sorting and Pagination
        sql.append(" ORDER BY u.username ASC LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add((int) pageable.getOffset());

        List<SecurityProfileReportDto> reportDtos = jdbcTemplate.query(
        	    sql.toString(), 
        	    (rs, rowNum) -> {
        	        String firstName = rs.getString("first_name");
        	        String lastName = rs.getString("last_name");
        	        String displayName = rs.getString("display_name");
        	        String username = rs.getString("username");
        	        String entraId = rs.getString("entra_id");
        	        String dbStatus = rs.getString("status");
        	        String roles = rs.getString("current_roles");

        	        // Format Display Name
        	        String formattedUser;
        	        if (displayName != null && !displayName.isBlank()) {
        	            formattedUser = displayName;
        	        } else if (lastName != null && firstName != null) {
        	            formattedUser = lastName + ", " + firstName;
        	        } else {
        	            formattedUser = username;
        	        }

        	        // Determine Account Type
        	        String accountType = "Unconfirmed";
        	        if (username != null && username.toUpperCase().startsWith("SVC")) {
        	            accountType = "Service";
        	        } else if (entraId != null && !entraId.isBlank()) {
        	            accountType = "Human";
        	        }

        	        return SecurityProfileReportDto.builder()
        	                .user(formattedUser)
        	                .userId(username)
        	                .type(accountType)
        	                .currentRoles(roles != null ? roles : "")
        	                .status(dbStatus != null ? dbStatus : "Active")
        	                .roleStartedOn(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().toLocalDate() : null)
        	                .lastAccessed(rs.getTimestamp("last_login_at") != null ? rs.getTimestamp("last_login_at").toLocalDateTime() : null)
        	                .lastTrainingCompleted(rs.getDate("training_completed_date") != null ? rs.getDate("training_completed_date").toLocalDate() : null)
        	                .trainingDue(rs.getDate("training_due_date") != null ? rs.getDate("training_due_date").toLocalDate() : null)
        	                .lastModifiedOn(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : rs.getTimestamp("created_at").toLocalDateTime())
        	                .lastModifiedBy(rs.getString("updated_by") != null ? rs.getString("updated_by") : rs.getString("created_by"))
        	                .build();
        	    }, 
        	    params.toArray() // 👈 Passed at the end as varargs instead of an explicit Object[] middle argument
        	);

        // Optional post-query filter for Account Type
        if (type != null && !type.isBlank() && !type.equalsIgnoreCase("All")) {
            reportDtos = reportDtos.stream()
                    .filter(dto -> dto.getType().equalsIgnoreCase(type))
                    .toList();
        }

        return new PageImpl<>(reportDtos, pageable, total);
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