package gov.fdic.tip.securityaudit.repository;

import gov.fdic.tip.securityaudit.entity.JvSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface JvSnapshotRepository extends JpaRepository<JvSnapshot, Long>, JpaSpecificationExecutor<JvSnapshot> {
    
    Page<JvSnapshot> findByManagedType(String managedType, Pageable pageable);
}