package gov.fdic.tip.securityaudit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "jv_snapshot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JvSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_pk")
    private Long snapshotPk;

    @NotBlank(message = "Snapshot type is required")
    @Size(max = 200, message = "Type cannot exceed 200 characters")
    @Column(name = "type", nullable = false, length = 200)
    private String type; // e.g., 'INITIAL', 'UPDATE'

    @NotNull(message = "Version is required")
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "state", columnDefinition = "TEXT")
    private String state;

    @Column(name = "changed_properties", columnDefinition = "TEXT")
    private String changedProperties;

    @NotBlank(message = "Managed type is required")
    @Size(max = 200, message = "Managed type cannot exceed 200 characters")
    @Column(name = "managed_type", nullable = false, length = 200)
    private String managedType;

    @Column(name = "global_id_fk")
    private Long globalIdFk;

    @Column(name = "commit_fk", insertable = false, updatable = false)
    private Long commitFk;

    // 👈 Map commit_fk as a ManyToOne relationship so we can filter by commitDate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_fk")
    private JvCommit commit;
}