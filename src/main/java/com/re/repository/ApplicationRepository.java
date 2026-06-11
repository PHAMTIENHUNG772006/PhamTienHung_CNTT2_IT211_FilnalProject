package com.re.repository;

import com.re.model.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application,Long> {
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    @Query("""
       SELECT a
       FROM Application a
       WHERE a.id = :applicationId
       AND a.job.company.owner.id = :userId
       """)
    Optional<Application> findApplicationForEmployer(
            Long applicationId,
            Long userId
    );

}
