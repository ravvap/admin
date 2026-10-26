package gov.fdic.tip.securityaudit.repository;

/**
 * Projection for the native grid query. Getter names match the quoted
 * camelCase aliases in SecurityTransactionLogQueries.GRID_SQL.
 */
public interface SecurityTransactionLogRow {

    String getAffectedUser();     // app_user.display_name

    String getUserName();         // app_user.username

    String getModifiedOn();

    String getModifiedBy();

    String getTransactionType();

    String getAttributeChanged();

    String getPreviousValue();

    String getNewValue();

    String getDetails();

    Long getTotalCount();
}
