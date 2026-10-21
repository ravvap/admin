package gov.fdic.tip.securityaudit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDateTime;

@Entity
@Table(name = "jv_commit")
@Getter
public class JvCommit {
    @Id
    @Column(name = "commit_pk")
    private Long commitPk;

    @Column(name = "commit_date")
    private LocalDateTime commitDate;
}