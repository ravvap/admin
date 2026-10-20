package gov.fdic.tip.securityaudit.repository;

import gov.fdic.tip.securityaudit.entity.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long>, JpaSpecificationExecutor<AccessLog> {
    
    // Additional custom finder if needed for quick date-range checks
    List<AccessLog> findByLoginDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
}