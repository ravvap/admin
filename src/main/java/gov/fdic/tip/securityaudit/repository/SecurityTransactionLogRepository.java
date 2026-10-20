package gov.fdic.tip.securityaudit.repository;

import gov.fdic.tip.securityaudit.entity.SecurityTransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityTransactionLogRepository 
        extends JpaRepository<SecurityTransactionLog, Long>, JpaSpecificationExecutor<SecurityTransactionLog> {
}