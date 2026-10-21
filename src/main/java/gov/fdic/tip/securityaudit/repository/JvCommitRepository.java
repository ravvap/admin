package gov.fdic.tip.securityaudit.repository;

import gov.fdic.tip.securityaudit.entity.JvCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JvCommitRepository extends JpaRepository<JvCommit, Long> {
    // You can add custom commit query methods here if needed, such as finding commits by date ranges
}