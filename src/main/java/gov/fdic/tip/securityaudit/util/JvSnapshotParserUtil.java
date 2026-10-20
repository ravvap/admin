package gov.fdic.tip.securityaudit.util;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class JvSnapshotParserUtil {

    private final ObjectMapper objectMapper;

    public String extractFieldFromJson(String stateJson, String fieldName) {
        try {
            if (stateJson == null || fieldName == null) {
                return "-";
            }
            JsonNode node = objectMapper.readTree(stateJson);
            if (node.has(fieldName)) {
                return node.get(fieldName).asText();
            }
        } catch (Exception e) {
            log.warn("Failed to parse state JSON for field: {}", fieldName, e);
        }
        return "-";
    }
}