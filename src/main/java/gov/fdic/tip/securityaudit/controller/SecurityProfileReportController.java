package gov.fdic.tip.securityaudit.controller;

import gov.fdic.tip.securityaudit.dto.SecurityProfileReportDto;
import gov.fdic.tip.securityaudit.service.SecurityProfileReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/security-audit/security-profiles")
@RequiredArgsConstructor
public class SecurityProfileReportController {

    private final SecurityProfileReportService securityProfileReportService;

    @GetMapping
    public ResponseEntity<Page<SecurityProfileReportDto>> getSecurityProfileReport(
            @RequestParam(required = false) String search,     // Matches User search box[cite: 40]
            @RequestParam(required = false) String type,       // Human, Service, Unconfirmed[cite: 40]
            @RequestParam(required = false) String role,       // Filter by specific role[cite: 40]
            @RequestParam(required = false) String status,     // Active, Disabled, Training Pending, etc.[cite: 40]
            @PageableDefault(size = 25, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<SecurityProfileReportDto> report = securityProfileReportService.getSecurityProfileReport(
                search, type, role, status, pageable
        );
        return ResponseEntity.ok(report);
    }
}