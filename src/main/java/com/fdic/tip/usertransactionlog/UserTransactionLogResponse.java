package com.fdic.tip.usertransactionlog.dto;

import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.DISPLAY_ZONE;

import com.fdic.tip.usertransactionlog.entity.TransactionType;
import com.fdic.tip.usertransactionlog.entity.UserTransactionLog;
import java.time.ZonedDateTime;

public record UserTransactionLogResponse(
        Long id,
        String userId,
        ZonedDateTime changedAt,
        Integer certNumber,
        String assessmentPeriod,
        String fieldName,
        TransactionType transactionType,
        String reasonCode,
        String reasonDescription,
        String previousValue,
        String currentValue) {

    public static UserTransactionLogResponse from(UserTransactionLog e) {
        return new UserTransactionLogResponse(
                e.getId(),
                e.getUserId(),
                e.getChangedAt().atZoneSameInstant(DISPLAY_ZONE),
                e.getCertNumber(),
                e.getAssessmentPeriod(),
                e.getFieldName(),
                e.getTransactionType(),
                e.getReasonCode(),
                e.getReasonDescription(),
                e.getPreviousValue(),
                e.getCurrentValue());
    }
}
