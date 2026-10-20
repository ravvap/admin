package gov.fdic.tip.securityaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityProfileReportDto {
    private String user;                  // Display name / Name (e.g., "Price, Jordan")
    private String userId;                // Username / User ID (e.g., "JPRICE")
    private String type;                  // Human, Service, or Unconfirmed[cite: 40]
    private String currentRoles;          // Comma-separated roles (e.g., "ASA")[cite: 40]
    private String status;                // Training Pending, Disabled, Active, Inactive, Suspended[cite: 40]
    private LocalDate roleStartedOn;      
    private LocalDateTime lastAccessed;   // Last login / accessed timestamp[cite: 40]
    private LocalDate lastTrainingCompleted;
    private LocalDate trainingDue;
    private LocalDateTime lastModifiedOn;
    private String lastModifiedBy;
}