package gov.fdic.tip.securityaudit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sa_splunk_event_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplunkEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_name", length = 100)
    private String alertName; // e.g., TIP-Failed-Login-Alert or TIP Atypical Hours Login

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime; // Mapped from Splunk _time

    @Column(length = 100)
    private String operation; // Mapped from Operation

    @Column(length = 100)
    private String signature; // Mapped from signature

    @Column(name = "request_type", length = 100)
    private String requestType; // Mapped from RequestType

    @Column(name = "user_id", length = 150)
    private String userId; // Mapped from UserId

    @Column(name = "actor_id", length = 255)
    private String actorId; // Mapped from Actor{}.ID / ActorId

    @Column(name = "splunk_id", length = 255)
    private String splunkId; // Mapped from Id

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}