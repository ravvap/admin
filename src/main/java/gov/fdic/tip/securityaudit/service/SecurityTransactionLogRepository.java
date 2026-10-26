package gov.fdic.tip.securityaudit.repository;

import gov.fdic.tip.securityaudit.entity.SecurityTransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SecurityTransactionLogRepository
        extends JpaRepository<SecurityTransactionLog, Long>, JpaSpecificationExecutor<SecurityTransactionLog> {

    /**
     * Security Transaction Log grid. Paging is done inside the SQL (LIMIT/OFFSET);
     * the total row count comes back on every row as totalCount.
     */
    @Query(value = SecurityTransactionLogQueries.GRID_SQL, nativeQuery = true)
    List<SecurityTransactionLogRow> findSecurityTransactionGrid(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("affectedUser") String affectedUser,
            @Param("changedBy") String changedBy,
            @Param("transactionType") String transactionType,
            @Param("pageSize") int pageSize,
            @Param("pageOffset") long pageOffset
    );
}
