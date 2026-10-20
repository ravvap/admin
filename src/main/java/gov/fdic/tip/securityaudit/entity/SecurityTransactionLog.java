package gov.fdic.tip.securityaudit.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "security_transaction_log", indexes = {
    @Index(name = "idx_sec_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_sec_log_event_type", columnList = "event_type"),
    @Index(name = "idx_sec_log_actor_id", columnList = "actor_id"),
    @Index(name = "idx_sec_log_outcome", columnList = "outcome")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Timestamp is required")
    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @NotBlank(message = "Event type is required")
    @Size(max = 100, message = "Event type cannot exceed 100 characters")
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Size(max = 100, message = "Module cannot exceed 100 characters")
    @Column(name = "module", length = 100)
    private String module;

    @Size(max = 100, message = "Target entity type cannot exceed 100 characters")
    @Column(name = "target_entity_type", length = 100)
    private String targetEntityType;

    @Size(max = 255, message = "Target entity label cannot exceed 255 characters")
    @Column(name = "target_entity_label", length = 255)
    private String targetEntityLabel;

    @Size(max = 50, message = "Actor type cannot exceed 50 characters")
    @Column(name = "actor_type", length = 50)
    private String actorType;

    @Size(max = 100, message = "Actor ID cannot exceed 100 characters")
    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Size(max = 255, message = "Actor label cannot exceed 255 characters")
    @Column(name = "actor_label", length = 255)
    private String actorLabel;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // Stores JSON payload details

    @Size(max = 50, message = "Outcome cannot exceed 50 characters")
    @Column(name = "outcome", length = 50)
    private String outcome;

    @Size(max = 45, message = "Source IP cannot exceed 45 characters")
    @Column(name = "source_ip", length = 45)
    private String sourceIp;
}