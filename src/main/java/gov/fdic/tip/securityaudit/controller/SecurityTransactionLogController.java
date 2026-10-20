package gov.fdic.tip.securityaudit.controller;

import gov.fdic.tip.securityaudit.dto.SecurityTransactionLogDto;
import gov.fdic.tip.securityaudit.service.SecurityTransactionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@Validated
@RestController
@RequestMapping("/api/security-audit/security-transactions")
@RequiredArgsConstructor
public class SecurityTransactionLogController {

    private final SecurityTransactionLogService service;

    @GetMapping
    public ResponseEntity<Page<SecurityTransactionLogDto>> getSecurityTransactionLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String module,
            @PageableDefault(size = 25, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<SecurityTransactionLogDto> logs = service.getSecurityTransactionLogs(
                startDate, endDate, eventType, actorId, outcome, module, pageable
        );
        return ResponseEntity.ok(logs);
    }
}