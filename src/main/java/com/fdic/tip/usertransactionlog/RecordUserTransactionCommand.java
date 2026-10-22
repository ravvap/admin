package com.fdic.tip.usertransactionlog.dto;

import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.*;

import com.fdic.tip.usertransactionlog.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/** Internal command used by producers (e.g. the change-history listener) to append a log entry. */
public record RecordUserTransactionCommand(
        @NotBlank @Size(max = USER_ID_MAX) String userId,
        @NotNull OffsetDateTime changedAt,
        @NotNull @Positive Integer certNumber,
        @NotBlank @Pattern(regexp = ASSESSMENT_PERIOD_PATTERN) String assessmentPeriod,
        @NotBlank @Size(max = FIELD_NAME_MAX) String fieldName,
        @NotNull TransactionType transactionType,
        @NotBlank @Size(max = REASON_CODE_MAX) String reasonCode,
        @NotBlank @Size(max = REASON_DESCRIPTION_MAX) String reasonDescription,
        @Size(max = VALUE_MAX) String previousValue,
        @Size(max = VALUE_MAX) String currentValue) {
}
