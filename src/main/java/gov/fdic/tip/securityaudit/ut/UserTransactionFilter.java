package gov.tip.admin.securityaudit.dto;

import static gov.tip.admin.securityaudit.constants.UserTransactionConstants.*;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Query-string filters from the screen. transactionType is free-form (CREATE, UPDATE, DELETE, READ, ...). */
public record UserTransactionFilter(
        @Size(max = USER_ID_MAX) String user,
        @Positive Integer certNumber,
        @Size(max = FIELD_NAME_MAX) String fieldName,
        @Pattern(regexp = TRANSACTION_TYPE_PATTERN) String transactionType,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changedTo,
        @Pattern(regexp = ASSESSMENT_PERIOD_PATTERN) String assessmentPeriod) {
}
