package com.re.repository;

import com.re.model.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job,Long> {
    Optional<Job> findById(Long id);
    @Query("""
       SELECT j
       FROM Job j
       WHERE j.id = :jobId
       AND j.company.owner.id = :userId
       """)
    Optional<Job> findJobForEmployer(
            @Param("jobId") Long jobId,
            @Param("userId") Long userId
    );
}
