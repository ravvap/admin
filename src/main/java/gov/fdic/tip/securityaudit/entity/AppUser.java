package gov.fdic.tip.securityaudit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idp_subject", length = 255)
    private String idpSubject;

    @Column(name = "username", length = 150, nullable = false, unique = true)
    private String username;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "first_name", length = 32)
    private String firstName;

    @Column(name = "last_name", length = 32)
    private String lastName;

    @Column(name = "entra_id", length = 255)
    private String entraId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "training_due_date")
    private LocalDateTime trainingDueDate;

    @Column(name = "training_completed_date")
    private LocalDateTime trainingCompletedDate;

    @Column(name = "training_expiry_date")
    private LocalDateTime trainingExpiryDate;

    @Column(name = "activated_by", length = 150)
    private String activatedBy;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @Column(name = "disabled_by", length = 150)
    private String disabledBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 150)
    private String deletedBy;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}