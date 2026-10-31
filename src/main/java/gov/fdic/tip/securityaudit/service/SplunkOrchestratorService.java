package gov.fdic.tip.securityaudit.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SplunkOrchestratorService {

    private final SplunkIngestionService ingestionService;

    // Your 10 distinct Splunk query strings
    private final Map<String, String> queryRegistry = Map.of(
        "TIP-Failed-Login-Alert", "index=\"office365-dev\" ...",
        "TIP-Atypical-Hours-Login", "index=\"office365-dev\" ..."
        // ... add remaining queries up to 10
    );

    @Async("splunkTaskExecutor") // 👈 Runs concurrently on a separate thread pool
    public CompletableFuture<Void> runAllQueriesConcurrently() {
        List<CompletableFuture<Void>> futures = queryRegistry.entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(() -> {
                log.info("Running query concurrently for: {}", entry.getKey());
                int count = ingestionService.fetchAndStoreSplunkAlerts(entry.getKey(), entry.getValue());
                log.info("Finished query: {} | Saved: {} records", entry.getKey(), count);
            }))
            .collect(Collectors.toList());

        // Wait for all concurrent queries to finish
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("All concurrent Splunk query ingestions completed.");
        return CompletableFuture.completedFuture(null);
    }
}