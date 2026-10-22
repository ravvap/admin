package com.fdic.tip.usertransactionlog.entity;

import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** One audited business-data change. Append-only: rows are never updated or deleted. */
@Entity
@Immutable
@Table(name = "user_transaction_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who made the change (user ID from TIP's change history). */
    @Column(name = "user_id", nullable = false, updatable = false, length = USER_ID_MAX)
    private String userId;

    /** When the change was saved. */
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    @Column(name = "cert_number", nullable = false, updatable = false)
    private Integer certNumber;

    /** Quarter the changed record belongs to (e.g. "2025 Q3"), not necessarily when the change was made. */
    @Column(name = "assessment_period", nullable = false, updatable = false, length = ASSESSMENT_PERIOD_MAX)
    private String assessmentPeriod;

    @Column(name = "field_name", nullable = false, updatable = false, length = FIELD_NAME_MAX)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false, length = TRANSACTION_TYPE_MAX)
    private TransactionType transactionType;

    @Column(name = "reason_code", nullable = false, updatable = false, length = REASON_CODE_MAX)
    private String reasonCode;

    @Column(name = "reason_description", nullable = false, updatable = false, length = REASON_DESCRIPTION_MAX)
    private String reasonDescription;

    @Column(name = "previous_value", updatable = false, length = VALUE_MAX)
    private String previousValue;

    @Column(name = "current_value", updatable = false, length = VALUE_MAX)
    private String currentValue;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
