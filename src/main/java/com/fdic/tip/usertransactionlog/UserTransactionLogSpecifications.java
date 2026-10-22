package com.fdic.tip.usertransactionlog.specification;

import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.DISPLAY_ZONE;

import com.fdic.tip.usertransactionlog.dto.UserTransactionLogFilter;
import com.fdic.tip.usertransactionlog.entity.UserTransactionLog;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class UserTransactionLogSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private UserTransactionLogSpecifications() {
    }

    public static Specification<UserTransactionLog> withFilter(UserTransactionLogFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.user())) {
                String term = escapeLike(filter.user().trim().toLowerCase(Locale.ROOT));
                predicates.add(cb.like(cb.lower(root.<String>get("userId")), "%" + term + "%", LIKE_ESCAPE));
            }
            if (filter.certNumber() != null) {
                predicates.add(cb.equal(root.get("certNumber"), filter.certNumber()));
            }
            if (hasText(filter.fieldName())) {
                predicates.add(cb.equal(root.get("fieldName"), filter.fieldName().trim()));
            }
            if (filter.transactionType() != null) {
                predicates.add(cb.equal(root.get("transactionType"), filter.transactionType()));
            }
            if (hasText(filter.assessmentPeriod())) {
                predicates.add(cb.equal(root.get("assessmentPeriod"), filter.assessmentPeriod().trim()));
            }
            // Date range is inclusive of both days, evaluated in Eastern Time (what the screen shows)
            if (filter.changedFrom() != null) {
                OffsetDateTime from = filter.changedFrom().atStartOfDay(DISPLAY_ZONE).toOffsetDateTime();
                predicates.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("changedAt"), from));
            }
            if (filter.changedTo() != null) {
                OffsetDateTime toExclusive = filter.changedTo().plusDays(1).atStartOfDay(DISPLAY_ZONE).toOffsetDateTime();
                predicates.add(cb.lessThan(root.<OffsetDateTime>get("changedAt"), toExclusive));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
