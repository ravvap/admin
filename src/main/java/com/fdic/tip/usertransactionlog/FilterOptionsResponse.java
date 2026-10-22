package com.fdic.tip.usertransactionlog.dto;

import com.fdic.tip.usertransactionlog.entity.TransactionType;
import java.util.List;

/** Values for the Assessment Period, Field and Transaction Type dropdowns. */
public record FilterOptionsResponse(
        List<String> assessmentPeriods,
        List<String> fieldNames,
        List<TransactionType> transactionTypes) {
}
