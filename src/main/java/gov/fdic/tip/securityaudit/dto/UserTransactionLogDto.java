package gov.fdic.tip.securityaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTransactionLogDto {
    private Long snapshotPk;
    private String type;                      // INITIAL or UPDATE
    private Long version;
    private String recordType;                // type_name from jv_global_id
    private String recordId;                  // local_id from jv_global_id[cite: 15]
    private String transactionExecuted;       // Commit timestamp
    private String changedProperties;         // Properties changed during update
    private String certNum;                   // Certificate number extracted from state
    private String assessmentPeriod;          // Assessment period extracted from state
    private String transactionReasonCode;     // Reason code extracted from state
    private String transactionDescription;    // Description extracted from state
    private Map<String, Object> previousValues; // Previous version state values
    private Map<String, Object> currentValue;   // Current/updated state values
}