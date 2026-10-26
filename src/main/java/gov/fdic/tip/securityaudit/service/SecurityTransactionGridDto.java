package gov.fdic.tip.securityaudit.dto;

import lombok.Builder;

/** One row of the Security Transaction Log grid. */
@Builder
public record SecurityTransactionGridDto(
        String affectedUser,
        String userId,          // app_user.username
        String modifiedOn,      // MM/DD/YYYY hh:mm AM, Eastern
        String modifiedBy,
        String transactionType,
        String attributeChanged,
        String previousValue,
        String newValue
) {
}
