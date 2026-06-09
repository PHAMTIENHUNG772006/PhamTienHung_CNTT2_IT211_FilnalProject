package com.re.repository;

import com.re.model.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application,Long> {
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);
}
