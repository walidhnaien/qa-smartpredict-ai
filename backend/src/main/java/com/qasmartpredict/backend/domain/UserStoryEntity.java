package com.qasmartpredict.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_story")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserStoryEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "release_id", nullable = false)
    private ReleaseEntity release;

    @Column(name = "jira_key", nullable = false, unique = true)
    private String jiraKey;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String status;

    private String priority;

    private String sprint;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
	
	@Column(name = "issue_type")
	private String issueType;

	private String reporter;

	private String assignee;
	@Column(name = "ts_ticket_id")
    private String tsTicketId;

	@Column(name = "created_date")
	private LocalDateTime createdDate;

	@Column(name = "resolved_date")
	private LocalDateTime resolvedDate;
	
	@ManyToMany(mappedBy = "userStories")
    private Set<RequirementEntity> requirements = new HashSet<>();
}