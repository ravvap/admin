package com.fdic.tip.usertransactionlog.controller;

import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.*;

import com.fdic.tip.usertransactionlog.dto.FilterOptionsResponse;
import com.fdic.tip.usertransactionlog.dto.PagedResponse;
import com.fdic.tip.usertransactionlog.dto.UserTransactionLogFilter;
import com.fdic.tip.usertransactionlog.dto.UserTransactionLogResponse;
import com.fdic.tip.usertransactionlog.service.UserTransactionLogService;
import jakarta.validation.Valid;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Read-only API. Authorization is enforced at URL level in SecurityConfig
 * (view: AUD, SYS_ADMIN; export: SYS_ADMIN only).
 */
@RestController
@RequestMapping(BASE_PATH)
@Validated
@RequiredArgsConstructor
public class UserTransactionLogController {

    private final UserTransactionLogService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserTransactionLogResponse recordTransaction(@Valid @RequestBody RecordUserTransactionCommand command) {
        return service.recordTransaction(command);
    }

    @GetMapping
    public PagedResponse<UserTransactionLogResponse> search(
            @Valid @ModelAttribute UserTransactionLogFilter filter,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = DEFAULT_SORT_FIELD, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.search(filter, pageable);
    }

    @GetMapping(ID_PATH)
    public UserTransactionLogResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping(FILTER_OPTIONS_PATH)
    public FilterOptionsResponse getFilterOptions() {
        return service.getFilterOptions();
    }

    @GetMapping(EXPORT_PATH)
    public ResponseEntity<StreamingResponseBody> export(@Valid @ModelAttribute UserTransactionLogFilter filter) {
        service.assertExportable(filter);

        StreamingResponseBody body = out -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                service.writeCsv(filter, writer);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(CSV_FILE_NAME).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
