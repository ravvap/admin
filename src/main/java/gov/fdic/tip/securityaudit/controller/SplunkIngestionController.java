package gov.fdic.tip.securityaudit.controller;

import gov.fdic.tip.securityaudit.service.SplunkIngestionService;
import gov.fdic.tip.securityaudit.service.SplunkOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/security-audit/splunk-ingestion")
@RequiredArgsConstructor
public class SplunkIngestionController {

    private final SplunkOrchestratorService orchestratorService;
    private final SplunkIngestionService ingestionService;

    /**
     * Endpoint called by your external scheduler service daily to trigger all 
     * configured Splunk queries concurrently.
     */
    @PostMapping("/trigger-all")
    public ResponseEntity<?> triggerAllIngestion() {
        log.info("Received request from external scheduler to run all Splunk query ingestions concurrently.");
        
        // Triggers the asynchronous orchestrator across all queries
        orchestratorService.runAllQueriesConcurrently();

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Batch ingestion triggered successfully for all configured Splunk queries."
        ));
    }

    /**
     * Endpoint to trigger a single, specific Splunk query on-demand.
     */
    @PostMapping("/trigger")
    public ResponseEntity<?> triggerSingleIngestion(
            @RequestParam String alertName,
            @RequestParam String searchQuery
    ) {
        log.info("Received request to run individual Splunk query for alert: {}", alertName);
        int recordsSaved = ingestionService.fetchAndStoreSplunkAlerts(alertName, searchQuery);

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "alertName", alertName,
            "recordsSaved", recordsSaved
        ));
    }
}