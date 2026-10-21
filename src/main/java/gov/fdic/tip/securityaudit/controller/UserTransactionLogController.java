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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime changedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime changedTo,
            @RequestParam(required = false) String certNum,
            @RequestParam(required = false) String assessmentPeriod,
            @PageableDefault(size = 25, sort = "snapshot_pk", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserTransactionLogDto> transactions = userTransactionLogService.getUserTransactions(
                user, fieldName, transactionType, changedFrom, changedTo, certNum, assessmentPeriod, pageable
        );
        return ResponseEntity.ok(transactions);
    }
}