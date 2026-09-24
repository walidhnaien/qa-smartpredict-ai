package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.RequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequirementRepository
        extends JpaRepository<RequirementEntity, UUID> {

    Optional<RequirementEntity> findByCode(String code);
	List<RequirementEntity> findDistinctByReleases_Id(UUID releaseId);

    /**
     * Retourne les requirements liés à au moins
     * une User Story appartenant à la Release.
     */
    @Query("""
        SELECT DISTINCT r
        FROM RequirementEntity r
        JOIN r.userStories us
        WHERE us.release.id = :releaseId
    """)
    List<RequirementEntity> findAllByReleaseId(
            @Param("releaseId") UUID releaseId
    );
	
	
	
	
	
	
	
}