package gov.fdic.tip.securityaudit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs", indexes = {
    @Index(name = "idx_login_time", columnList = "loginDateTime"),
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_account_type", columnList = "accountType"),
    @Index(name = "idx_login_successful", columnList = "loginSuccessful"),
    @Index(name = "idx_create_date", columnList = "createDate")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Column(name = "login_date_time", nullable = false)
    private LocalDateTime loginDateTime;

    @Column(name = "logout_date_time")
    private LocalDateTime logoutDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "logout_status", nullable = false, length = 30)
    private LogoutStatus logoutStatus;

    @Column(name = "login_successful", nullable = false)
    private Boolean loginSuccessful;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 30)
    private AccessType accessType;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        // If createdBy is not set programmatically, fallback to system user or ID
        if (this.createdBy == null) {
            this.createdBy = "SYSTEM";
        }
    }

    public enum AccountType {
        HUMAN, SERVICE, UNCONFIRMED
    }

    public enum LogoutStatus {
        OPEN, COMPLETED, NOT_APPLICABLE
    }

    public enum AccessType {
        ONLINE, SQL_ACCESS
    }
}