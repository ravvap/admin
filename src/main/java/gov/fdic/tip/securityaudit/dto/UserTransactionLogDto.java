package gov.fdic.tip.securityaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTransactionLogDto {

    private Long snapshotPk;
    private String user;
    private LocalDateTime dateTime;
    private String certNumber;
    private String assessmentPeriod;
    private String field;
    private String transactionType; // INITIAL or UPDATE
    private String transactionReasonCode;
    private String transactionDescription;
    private String previousValue;
    private String currentValue;

    // Additional fields required during export (US-UT-5)
    private String recordType;
    private Long recordId;
    private Long commitId;
}