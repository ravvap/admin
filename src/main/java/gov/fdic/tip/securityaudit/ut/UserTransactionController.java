package gov.tip.admin.securityaudit.controller;

import static gov.tip.admin.securityaudit.constants.UserTransactionConstants.*;

import gov.tip.admin.securityaudit.dto.FilterOptionsResponse;
import gov.tip.admin.securityaudit.dto.PagedResponse;
import gov.tip.admin.securityaudit.dto.UserTransactionFilter;
import gov.tip.admin.securityaudit.dto.UserTransactionResponse;
import gov.tip.admin.securityaudit.service.UserTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only User Transaction Log. Authorization is enforced at URL level in SecurityConfig
 * (view: AUD, SYS_ADMIN).
 */
@RestController
@RequestMapping(BASE_PATH)
@RequiredArgsConstructor
public class UserTransactionController {

    private final UserTransactionService service;

    @GetMapping
    public PagedResponse<UserTransactionResponse> search(
            @Valid @ModelAttribute UserTransactionFilter filter,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = DEFAULT_SORT_FIELD, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.search(filter, pageable);
    }

    @GetMapping(FILTER_OPTIONS_PATH)
    public FilterOptionsResponse getFilterOptions() {
        return service.getFilterOptions();
    }
}
