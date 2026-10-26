package gov.fdic.tip.securityaudit.controller;

import gov.fdic.tip.securityaudit.constants.SecurityAuditApiConstants;
import gov.fdic.tip.securityaudit.dto.SecurityTransactionGridDto;
import gov.fdic.tip.securityaudit.service.SecurityTransactionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * Security & Audit > Security Transactions grid.
 * Authorization is enforced at the SecurityFilterChain URL level (no @PreAuthorize).
 */
@Slf4j
@RestController
@RequestMapping(SecurityAuditApiConstants.BASE_PATH)
@RequiredArgsConstructor
public class SecurityTransactionController {

    private final SecurityTransactionLogService service;

    /**
     * GET /api/v1/security-audit/security-transactions
     *   ?from=2026-01-01&to=2026-03-31   (all parameters are optional)
     *   &affectedUser=keller        (optional, matches display name or username)
     *   &changedBy=carter           (optional)
     *   &transactionType=ADD ROLE   (optional, omit or ALL for every type)
     *   &page=0&size=15
     */
    @GetMapping(SecurityAuditApiConstants.SECURITY_TRANSACTIONS_PATH)
    public ResponseEntity<Page<SecurityTransactionGridDto>> getSecurityTransactions(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "affectedUser", required = false) String affectedUser,
            @RequestParam(value = "changedBy", required = false) String changedBy,
            @RequestParam(value = "transactionType", required = false) String transactionType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "" + SecurityAuditApiConstants.DEFAULT_PAGE_SIZE) int size
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SecurityAuditApiConstants.MSG_DATE_RANGE_INVALID);
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), SecurityAuditApiConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        log.debug("Security transactions requested from={} to={} page={} size={}", from, to, safePage, safeSize);

        return ResponseEntity.ok(
                service.getSecurityTransactionGrid(from, to, affectedUser, changedBy, transactionType, pageable));
    }
}
