package gov.fdic.tip.securityaudit.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.fdic.tip.securityaudit.entity.AccessLog;
import gov.fdic.tip.securityaudit.service.AccessLogService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security-audit/access-logs")
@RequiredArgsConstructor
public class AccessLogController {

    private final AccessLogService accessLogService;

    @GetMapping
    public ResponseEntity<Page<AccessLog>> getAccessLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) AccessLog.AccountType accountType,
            @RequestParam(required = false, defaultValue = "false") Boolean hideServiceAccounts,
            @RequestParam(required = false) Boolean loginSuccessful,
            @RequestParam(required = false) AccessLog.AccessType accessType,
            @PageableDefault(size = 25, sort = "loginDateTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AccessLog> logs = accessLogService.getAccessLogs(
                startDate, endDate, userId, accountType, hideServiceAccounts, loginSuccessful, accessType, pageable
        );
        return ResponseEntity.ok(logs);
    }

     
}