package gov.fdic.tip.securityaudit.repository;

import gov.fdic.tip.securityaudit.entity.JvGlobalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JvGlobalIdRepository extends JpaRepository<JvGlobalId, Long> {
    // You can add custom finders here if needed, for example:
    // Optional<JvGlobalId> findByTypeNameAndLocalId(String typeName, String localId);
}