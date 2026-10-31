package gov.fdic.tip.securityaudit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fdic.tip.securityaudit.entity.SplunkEventLog;
import gov.fdic.tip.securityaudit.repository.SplunkEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SplunkIngestionService {

    private final SplunkEventLogRepository eventLogRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${splunk.host:https://your-splunk-instance:8089}")
    private String splunkHost;

    @Value("${splunk.token:YOUR_SPLUNK_AUTH_TOKEN}")
    private String splunkToken;

    /**
     * Executes a Splunk search query, parses the JSON response, and stores records in DB.
     */
    @Transactional
    public int fetchAndStoreSplunkAlerts(String alertName, String searchQuery) {
        try {
            // Step 1: Dispatch search job
            String sid = createSearchJob(searchQuery);
            if (sid == null) {
                log.error("Failed to create Splunk search job for alert: {}", alertName);
                return 0;
            }

            // Step 2: Retrieve JSON search results
            String resultsJson = fetchJobResults(sid);
            if (resultsJson == null) {
                log.error("Failed to fetch results for Splunk job SID: {}", sid);
                return 0;
            }

            // Step 3: Parse JSON & perform batch database insert
            return parseAndSaveResults(alertName, resultsJson);

        } catch (Exception e) {
            log.error("Error executing Splunk ingestion pipeline for alert {}: {}", alertName, e.getMessage(), e);
            return 0;
        }
    }

    private String createSearchJob(String query) {
        String url = splunkHost + "/services/search/jobs?output_mode=json";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(splunkToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("search", query);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("sid").asText(null);
            } catch (Exception e) {
                log.error("Error parsing Splunk SID response: {}", e.getMessage());
            }
        }
        return null;
    }

    private String fetchJobResults(String sid) {
        String url = splunkHost + "/services/search/jobs/" + sid + "/results?output_mode=json&count=0";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(splunkToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error retrieving Splunk results for SID {}: {}", sid, e.getMessage());
        }
        return null;
    }

    private int parseAndSaveResults(String alertName, String jsonBody) {
        int savedCount = 0;
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode results = root.path("results");

            List<SplunkEventLog> batchList = new ArrayList<>();

            if (results.isArray()) {
                for (JsonNode row : results) {
                    SplunkEventLog eventLog = SplunkEventLog.builder()
                            .alertName(alertName)
                            .eventTime(parseEpochTime(row.path("_time").asText(null)))
                            .operation(getTextValue(row, "Operation"))
                            .signature(getTextValue(row, "signature"))
                            .requestType(getTextValue(row, "RequestType"))
                            .userId(getTextValue(row, "UserId"))
                            .actorId(getTextValue(row, "Actor{}.ID", "ActorId"))
                            .splunkId(getTextValue(row, "Id"))
                            .build();

                    batchList.add(eventLog);

                    // Execute batch inserts every 50 records
                    if (batchList.size() >= 50) {
                        eventLogRepository.saveAll(batchList);
                        savedCount += batchList.size();
                        batchList.clear();
                    }
                }

                if (!batchList.isEmpty()) {
                    eventLogRepository.saveAll(batchList);
                    savedCount += batchList.size();
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Splunk results JSON payload: {}", e.getMessage(), e);
        }
        return savedCount;
    }

    private String getTextValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode val = node.path(fieldName);
            if (!val.isMissingNode() && !val.isNull()) {
                return val.asText();
            }
        }
        return null;
    }

    private LocalDateTime parseEpochTime(String epochStr) {
        if (epochStr == null || epochStr.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            double epochSeconds = Double.parseDouble(epochStr);
            long seconds = (long) epochSeconds;
            long nanos = (long) ((epochSeconds - seconds) * 1_000_000_000);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanos), ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            return LocalDateTime.now();
        }
    }
}