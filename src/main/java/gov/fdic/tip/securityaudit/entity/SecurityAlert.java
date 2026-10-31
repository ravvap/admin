package gov.fdic.tip.securityaudit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sa_security_alert")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(name = "condition_name", nullable = false, length = 100)
    private String conditionName;

    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;

    @Column(name = "account_source", length = 150)
    private String accountSource;

    @Column(name = "first_detected", nullable = false)
    private LocalDateTime firstDetected;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertStatus status;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "handled_by", length = 100)
    private String handledBy;

    public enum Severity {
        CRITICAL, HIGH, MEDIUM
    }

    public enum AlertStatus {
        NEW, ACKNOWLEDGED, RESOLVED, DISMISSED
    }
}