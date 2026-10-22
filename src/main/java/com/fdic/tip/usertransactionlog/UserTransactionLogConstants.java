package com.fdic.tip.usertransactionlog.constants;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

public final class UserTransactionLogConstants {

    private UserTransactionLogConstants() {
    }

    // ---- API paths ----
    public static final String BASE_PATH = "/api/v1/user-transactions";
    public static final String ID_PATH = "/{id}";
    public static final String EXPORT_PATH = "/export";
    public static final String FILTER_OPTIONS_PATH = "/filter-options";

    // ---- Paging / sorting ----
    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "changedAt";
    public static final Set<String> SORTABLE_FIELDS = Set.of(
            "changedAt", "userId", "certNumber", "assessmentPeriod",
            "fieldName", "transactionType", "reasonCode");

    // ---- Field limits (shared by entity columns and DTO validation) ----
    public static final int USER_ID_MAX = 100;
    public static final int FIELD_NAME_MAX = 100;
    public static final int REASON_CODE_MAX = 20;
    public static final int REASON_DESCRIPTION_MAX = 255;
    public static final int VALUE_MAX = 4000;
    public static final int TRANSACTION_TYPE_MAX = 10;
    public static final int ASSESSMENT_PERIOD_MAX = 7;
    public static final String ASSESSMENT_PERIOD_PATTERN = "^\\d{4} Q[1-4]$";

    // ---- Display / export ----
    public static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");
    public static final String CSV_DATE_PATTERN = "MM/dd/yyyy h:mm a";
    public static final String CSV_FILE_NAME = "user-transaction-log.csv";
    public static final int EXPORT_BATCH_SIZE = 1000;
    public static final int EXPORT_MAX_ROWS = 100_000;
    public static final List<String> CSV_HEADERS = List.of(
            "User", "Date/Time (ET)", "Cert #", "Assessment Period", "Field",
            "Transaction Type", "Transaction Reason Code", "Transaction Description",
            "Previous Value", "Current Value");

    // ---- Messages ----
    public static final String MSG_NOT_FOUND = "User transaction %d was not found";
    public static final String MSG_INVALID_DATE_RANGE = "'changedFrom' must not be after 'changedTo'";
    public static final String MSG_EXPORT_TOO_LARGE =
            "Export exceeds the maximum of %d rows; narrow the filters and try again";
}
