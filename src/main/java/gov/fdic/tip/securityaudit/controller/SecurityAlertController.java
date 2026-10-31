package gov.fdic.tip.securityaudit.controller;

import gov.fdic.tip.securityaudit.dto.PaginatedResponse;
import gov.fdic.tip.securityaudit.dto.SecurityAlertDto;
import gov.fdic.tip.securityaudit.entity.SecurityAlert;
import gov.fdic.tip.securityaudit.service.SecurityAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/security-audit/alerts")
@RequiredArgsConstructor
public class SecurityAlertController {

    private final SecurityAlertService alertService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<SecurityAlertDto>> getAlerts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 25, sort = "firstDetected", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        Page<SecurityAlertDto> alertPage = alertService.getAlerts(search, condition, severity, status, startDate, endDate, pageable);

        PaginatedResponse<SecurityAlertDto> response = PaginatedResponse.<SecurityAlertDto>builder()
                .content(alertPage.getContent())
                .page(alertPage.getNumber())
                .size(alertPage.getSize())
                .totalElements(alertPage.getTotalElements())
                .totalPages(alertPage.getTotalPages())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SecurityAlertDto> getAlertDetail(@PathVariable Long id) {
        SecurityAlertDto detail = alertService.getAlertDetail(id);
        return ResponseEntity.ok(detail);
    }

    @PatchMapping("/{id}/triage")
    public ResponseEntity<SecurityAlertDto> triageAlert(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String resolutionNote,
            Principal principal
    ) {
        String username = (principal != null) ? principal.getName() : "SYSTEM";

        SecurityAlert.AlertStatus alertStatus = (status != null && !status.isBlank()) 
                ? SecurityAlert.AlertStatus.valueOf(status.trim().toUpperCase()) : null;
                
        SecurityAlert.Severity alertSeverity = (severity != null && !severity.isBlank()) 
                ? SecurityAlert.Severity.valueOf(severity.trim().toUpperCase()) : null;

        SecurityAlertDto updated = alertService.triageAlert(id, alertStatus, alertSeverity, resolutionNote, username);
        
        return ResponseEntity.ok(updated);
    }
}