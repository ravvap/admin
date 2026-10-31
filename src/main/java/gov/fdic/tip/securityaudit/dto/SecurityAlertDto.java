package gov.fdic.tip.securityaudit.dto;

import gov.fdic.tip.securityaudit.entity.SecurityAlert;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SecurityAlertDto {
    private Long id;
    private SecurityAlert.Severity severity;
    private String conditionName;
    private String triggerType;
    private String accountSource;
    private LocalDateTime firstDetected;
    private LocalDateTime lastSeen;
    private SecurityAlert.AlertStatus status;
    private String correlationId;
    private String resolutionNote;
    private String handledBy;

    public static SecurityAlertDto from(SecurityAlert entity) {
        return SecurityAlertDto.builder()
                .id(entity.getId())
                .severity(entity.getSeverity())
                .conditionName(entity.getConditionName())
                .triggerType(entity.getTriggerType())
                .accountSource(entity.getAccountSource())
                .firstDetected(entity.getFirstDetected())
                .lastSeen(entity.getLastSeen())
                .status(entity.getStatus())
                .correlationId(entity.getCorrelationId())
                .resolutionNote(entity.getResolutionNote())
                .handledBy(entity.getHandledBy())
                .build();
    }
}