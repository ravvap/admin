package com.fdic.tip.usertransactionlog.service;

import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.*;

import com.fdic.tip.usertransactionlog.dto.FilterOptionsResponse;
import com.fdic.tip.usertransactionlog.dto.PagedResponse;
import com.fdic.tip.usertransactionlog.dto.RecordUserTransactionCommand;
import com.fdic.tip.usertransactionlog.dto.UserTransactionLogFilter;
import com.fdic.tip.usertransactionlog.dto.UserTransactionLogResponse;
import com.fdic.tip.usertransactionlog.entity.TransactionType;
import com.fdic.tip.usertransactionlog.entity.UserTransactionLog;
import com.fdic.tip.usertransactionlog.repository.UserTransactionLogRepository;
import com.fdic.tip.usertransactionlog.specification.UserTransactionLogSpecifications;
import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserTransactionLogService {

    private static final DateTimeFormatter CSV_DATE_FORMAT =
            DateTimeFormatter.ofPattern(CSV_DATE_PATTERN, Locale.US);
    private static final String CSV_FORMULA_PREFIXES = "=+-@\t\r";

    private final UserTransactionLogRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<UserTransactionLogResponse> search(UserTransactionLogFilter filter, Pageable pageable) {
        validateFilter(filter);
        Page<UserTransactionLog> page =
                repository.findAll(UserTransactionLogSpecifications.withFilter(filter), sanitize(pageable));
        return PagedResponse.from(page.map(UserTransactionLogResponse::from));
    }

    @Transactional(readOnly = true)
    public UserTransactionLogResponse getById(Long id) {
        return repository.findById(id)
                .map(UserTransactionLogResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, String.format(MSG_NOT_FOUND, id)));
    }

    @Transactional(readOnly = true)
    public FilterOptionsResponse getFilterOptions() {
        return new FilterOptionsResponse(
                repository.findDistinctAssessmentPeriods(),
                repository.findDistinctFieldNames(),
                List.of(TransactionType.values()));
    }

    /** Appends one entry. Not exposed over REST: the log is system-generated. */
    @Transactional
    public UserTransactionLogResponse recordTransaction(RecordUserTransactionCommand cmd) {
        UserTransactionLog saved = repository.save(UserTransactionLog.builder()
                .userId(cmd.userId())
                .changedAt(cmd.changedAt())
                .certNumber(cmd.certNumber())
                .assessmentPeriod(cmd.assessmentPeriod())
                .fieldName(cmd.fieldName())
                .transactionType(cmd.transactionType())
                .reasonCode(cmd.reasonCode())
                .reasonDescription(cmd.reasonDescription())
                .previousValue(cmd.previousValue())
                .currentValue(cmd.currentValue())
                .build());
        return UserTransactionLogResponse.from(saved);
    }

    /** Call before streaming so a too-large export fails with a proper HTTP error. */
    @Transactional(readOnly = true)
    public void assertExportable(UserTransactionLogFilter filter) {
        validateFilter(filter);
        long count = repository.count(UserTransactionLogSpecifications.withFilter(filter));
        if (count > EXPORT_MAX_ROWS) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, String.format(MSG_EXPORT_TOO_LARGE, EXPORT_MAX_ROWS));
        }
    }

    @Transactional(readOnly = true)
    public void writeCsv(UserTransactionLogFilter filter, Writer writer) throws IOException {
        Specification<UserTransactionLog> spec = UserTransactionLogSpecifications.withFilter(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD).and(Sort.by(Sort.Direction.DESC, "id"));

        writer.write(String.join(",", CSV_HEADERS));
        writer.write("\r\n");

        int pageNumber = 0;
        long written = 0;
        Page<UserTransactionLog> page;
        do {
            page = repository.findAll(spec, PageRequest.of(pageNumber++, EXPORT_BATCH_SIZE, sort));
            for (UserTransactionLog row : page.getContent()) {
                writer.write(toCsvRow(row));
                writer.write("\r\n");
            }
            written += page.getNumberOfElements();
        } while (page.hasNext() && written < EXPORT_MAX_ROWS);
        writer.flush();
    }

    // ---- helpers ----

    private void validateFilter(UserTransactionLogFilter filter) {
        if (filter.changedFrom() != null && filter.changedTo() != null
                && filter.changedFrom().isAfter(filter.changedTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_INVALID_DATE_RANGE);
        }
    }

    /** Caps page size and only allows whitelisted sort fields; adds id as a stable tiebreaker. */
    private Pageable sanitize(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        List<Sort.Order> orders = pageable.getSort().stream()
                .filter(o -> SORTABLE_FIELDS.contains(o.getProperty()))
                .toList();
        Sort sort = orders.isEmpty() ? Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD) : Sort.by(orders);
        return PageRequest.of(pageable.getPageNumber(), size, sort.and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private String toCsvRow(UserTransactionLog r) {
        return String.join(",", Arrays.asList(
                csvCell(r.getUserId()),
                csvCell(r.getChangedAt().atZoneSameInstant(DISPLAY_ZONE).format(CSV_DATE_FORMAT)),
                csvCell(String.valueOf(r.getCertNumber())),
                csvCell(r.getAssessmentPeriod()),
                csvCell(r.getFieldName()),
                csvCell(r.getTransactionType().name()),
                csvCell(r.getReasonCode()),
                csvCell(r.getReasonDescription()),
                csvCell(r.getPreviousValue()),
                csvCell(r.getCurrentValue())));
    }

    /** RFC 4180 quoting, plus a leading apostrophe on cells that spreadsheets would treat as formulas. */
    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String cell = value;
        if (!cell.isEmpty() && CSV_FORMULA_PREFIXES.indexOf(cell.charAt(0)) >= 0) {
            cell = "'" + cell;
        }
        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
            cell = "\"" + cell.replace("\"", "\"\"") + "\"";
        }
        return cell;
    }
}
