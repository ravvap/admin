package gov.tip.admin.securityaudit.service;

import static gov.tip.admin.securityaudit.constants.UserTransactionConstants.*;

import gov.tip.admin.securityaudit.dto.FilterOptionsResponse;
import gov.tip.admin.securityaudit.dto.PagedResponse;
import gov.tip.admin.securityaudit.dto.UserTransactionFilter;
import gov.tip.admin.securityaudit.dto.UserTransactionResponse;
import gov.tip.admin.securityaudit.repository.UserTransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserTransactionService {

    private final UserTransactionRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<UserTransactionResponse> search(UserTransactionFilter filter, Pageable pageable) {
        validateFilter(filter);
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        long offset = (long) pageable.getPageNumber() * size;

        long total = repository.count(filter);
        List<UserTransactionResponse> rows =
                total > offset ? repository.findAll(filter, pageable.getSort(), size, offset) : List.of();
        return PagedResponse.of(rows, pageable.getPageNumber(), size, total);
    }

    @Transactional(readOnly = true)
    public FilterOptionsResponse getFilterOptions() {
        return new FilterOptionsResponse(
                repository.findDistinctAssessmentPeriods(),
                repository.findDistinctFieldNames(),
                repository.findDistinctTransactionTypes());
    }

    private void validateFilter(UserTransactionFilter filter) {
        if (filter.changedFrom() != null && filter.changedTo() != null
                && filter.changedFrom().isAfter(filter.changedTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_INVALID_DATE_RANGE);
        }
    }
}
