package gov.fdic.tip.securityaudit.repository;

import gov.fdic.tip.securityaudit.entity.SplunkEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SplunkEventLogRepository extends JpaRepository<SplunkEventLog, Long>, JpaSpecificationExecutor<SplunkEventLog> {
}