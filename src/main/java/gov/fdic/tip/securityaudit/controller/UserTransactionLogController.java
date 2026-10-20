package gov.fdic.tip.securityaudit.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Pattern; // ✅ This is the correct bean validation annotation
import gov.fdic.tip.securityaudit.dto.UserTransactionLogDto;
import gov.fdic.tip.securityaudit.service.UserTransactionLogService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/security-audit/user-transactions")
@RequiredArgsConstructor
public class UserTransactionLogController {

    private final UserTransactionLogService userTransactionLogService;

    @GetMapping
    public ResponseEntity<Page<UserTransactionLogDto>> getUserTransactionLogs(
            @RequestParam(required = false) 
            @Pattern(regexp = "^\\d{4}\\sQ[1-4]$", message = "Assessment period must be in format YYYY QX (e.g., 2025 Q3)") 
            String assessmentPeriod,

            @RequestParam(required = false) String userSearch,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String managedType,
            @PageableDefault(size = 25, sort = "snapshotPk", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserTransactionLogDto> logs = userTransactionLogService.getUserTransactionLogs(
                assessmentPeriod, userSearch, transactionType, managedType, pageable
        );
        return ResponseEntity.ok(logs);
    }
}