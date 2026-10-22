package com.fdic.tip.usertransactionlog.dto;

import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.*;

import com.fdic.tip.usertransactionlog.entity.TransactionType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Query-string filters from the screen: user, cert #, field, transaction type, changed-from/to, assessment period. */
public record UserTransactionLogFilter(
        @Size(max = USER_ID_MAX) String user,
        @Positive Integer certNumber,
        @Size(max = FIELD_NAME_MAX) String fieldName,
        TransactionType transactionType,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changedTo,
        @Pattern(regexp = ASSESSMENT_PERIOD_PATTERN) String assessmentPeriod) {
}
