package gov.fdic.tip.securityaudit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fdic.tip.securityaudit.dto.SecurityTransactionLogDto;
import gov.fdic.tip.securityaudit.entity.SecurityTransactionLog;
import gov.fdic.tip.securityaudit.repository.SecurityTransactionLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityTransactionLogService {

    private final SecurityTransactionLogRepository repository;
    private final ObjectMapper objectMapper;

    public Page<SecurityTransactionLogDto> getSecurityTransactionLogs(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String eventType,
            String actorId,
            String outcome,
            String module,
            Pageable pageable
    ) {
        Specification<SecurityTransactionLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("timestamp"), startDate, endDate));
            }

            if (eventType != null && !eventType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), eventType));
            }

            if (actorId != null && !actorId.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("actorId")), "%" + actorId.toLowerCase() + "%"));
            }

            if (outcome != null && !outcome.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("outcome")), outcome.toLowerCase()));
            }

            if (module != null && !module.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("module")), module.toLowerCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<SecurityTransactionLog> entityPage = repository.findAll(spec, pageable);

        List<SecurityTransactionLogDto> dtoList = entityPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }

    private SecurityTransactionLogDto mapToDto(SecurityTransactionLog entity) {
        String reason = "-";
        String message = "-";
        String newStatus = "-";
        String previousStatus = "-";

        if (entity.getDetails() != null) {
            try {
                JsonNode jsonNode = objectMapper.readTree(entity.getDetails());
                if (jsonNode.has("reason")) reason = jsonNode.get("reason").asText();
                if (jsonNode.has("message")) message = jsonNode.get("message").asText();
                if (jsonNode.has("newStatus")) newStatus = jsonNode.get("newStatus").asText();
                if (jsonNode.has("previousStatus")) previousStatus = jsonNode.get("previousStatus").asText();
            } catch (Exception e) {
                log.warn("Failed to parse security log details JSON for id: {}", entity.getId(), e);
            }
        }

        return SecurityTransactionLogDto.builder()
                .id(entity.getId())
                .timestamp(entity.getTimestamp())
                .eventType(entity.getEventType())
                .module(entity.getModule())
                .targetEntityType(entity.getTargetEntityType())
                .targetEntityLabel(entity.getTargetEntityLabel())
                .actorType(entity.getActorType())
                .actorId(entity.getActorId())
                .actorLabel(entity.getActorLabel())
                .outcome(entity.getOutcome())
                .sourceIp(entity.getSourceIp())
                .reason(reason)
                .message(message)
                .newStatus(newStatus)
                .previousStatus(previousStatus)
                .build();
    }
}