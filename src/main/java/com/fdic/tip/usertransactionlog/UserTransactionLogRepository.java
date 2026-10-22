package com.fdic.tip.usertransactionlog.repository;

import com.fdic.tip.usertransactionlog.entity.UserTransactionLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface UserTransactionLogRepository
        extends JpaRepository<UserTransactionLog, Long>, JpaSpecificationExecutor<UserTransactionLog> {

    @Query("select distinct u.assessmentPeriod from UserTransactionLog u order by u.assessmentPeriod desc")
    List<String> findDistinctAssessmentPeriods();

    @Query("select distinct u.fieldName from UserTransactionLog u order by u.fieldName asc")
    List<String> findDistinctFieldNames();
}
