package gov.fdic.tip.securityaudit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import gov.fdic.tip.securityaudit.entity.AppUser;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    @Query("SELECT u, r.code FROM AppUser u LEFT JOIN UserRole ur ON u.id = ur.userId LEFT JOIN AppRole r ON ur.roleId = r.id")
    List<Object[]> findAllUsersWithRoles();
}