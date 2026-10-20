package gov.fdic.tip.securityaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityTransactionLogDto {
    private Long id;
    private OffsetDateTime timestamp;
    private String eventType;
    private String module;
    private String targetEntityType;
    private String targetEntityLabel;
    private String actorType;
    private String actorId;
    private String actorLabel;
    private String outcome;
    private String sourceIp;
    
    // Extracted attributes from JSON 'details'
    private String reason;
    private String message;
    private String newStatus;
    private String previousStatus;
}