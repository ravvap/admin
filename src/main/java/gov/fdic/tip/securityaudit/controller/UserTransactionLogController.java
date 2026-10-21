package gov.fdic.tip.securityaudit.controller;

import gov.fdic.tip.securityaudit.dto.UserTransactionLogDto;
import gov.fdic.tip.securityaudit.service.UserTransactionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/api/security-audit/user-transactions")
@RequiredArgsConstructor
public class UserTransactionLogController {

    private final UserTransactionLogService userTransactionLogService;

    @GetMapping
    public ResponseEntity<Page<UserTransactionLogDto>> getUserTransactions(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "MM/dd/yyyy") LocalDate changedFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "MM/dd/yyyy") LocalDate changedTo,
            @RequestParam(required = false) String certNum,
            @RequestParam(required = false) String assessmentPeriod,
            @PageableDefault(size = 25, sort = "snapshot_pk", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // Convert LocalDate inputs to LocalDateTime boundaries for accurate timestamp range querying
        LocalDateTime startDateTime = (changedFrom != null) ? changedFrom.atStartOfDay() : null;
        LocalDateTime endDateTime = (changedTo != null) ? changedTo.atTime(23, 59, 59) : null;

        Page<UserTransactionLogDto> transactions = userTransactionLogService.getUserTransactions(
                user, fieldName, transactionType, startDateTime, endDateTime, certNum, assessmentPeriod, pageable
        );
        return ResponseEntity.ok(transactions);
    }
}