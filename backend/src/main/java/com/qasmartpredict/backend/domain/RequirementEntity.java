package com.qasmartpredict.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "requirement")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RequirementEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private ApplicationEntity application;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String criticality;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
	
	@JsonIgnore
	@ManyToMany
    @JoinTable(name = "requirement_user_story",joinColumns = @JoinColumn(name = "requirement_id"),inverseJoinColumns = @JoinColumn(name = "user_story_id"))
    private Set<UserStoryEntity> userStories = new HashSet<>();
	
}