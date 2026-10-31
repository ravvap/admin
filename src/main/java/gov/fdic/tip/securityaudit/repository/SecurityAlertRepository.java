package gov.fdic.tip.securityaudit.repository;

import gov.fdic.tip.securityaudit.entity.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long>, JpaSpecificationExecutor<SecurityAlert> {
}