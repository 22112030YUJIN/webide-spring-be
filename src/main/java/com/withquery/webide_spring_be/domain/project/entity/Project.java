package com.withquery.webide_spring_be.domain.project.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.withquery.webide_spring_be.domain.collaboration.entity.ProjectInvitation;
import com.withquery.webide_spring_be.domain.collaboration.entity.ProjectMember;
import com.withquery.webide_spring_be.domain.file.entity.File;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "프로젝트 엔티티")
public class Project {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "프로젝트 고유 ID", example = "001")
	private Long id;

	@Column(nullable = false, length = 100)
	@Schema(description = "프로젝트 이름", example = "My Project", maxLength = 100)
	private String name;

	@Column(length = 500)
	@Schema(description = "프로젝트 설명", example = "데이터 분석을 위한 SQL 프로젝트입니다.", maxLength = 500)
	private String description;

	@Column(name = "owner_id", nullable = false)
	@Schema(description = "프로젝트 소유자 ID", example = "123")
	private Long ownerId;

	@Column(name = "is_public", nullable = false)
	@Schema(description = "프로젝트 공개 여부", example = "true")
	private Boolean isPublic;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	@Schema(description = "프로젝트 생성 일시", example = "2025-07-22 10:30:00")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	@Schema(description = "프로젝트 수정 일시", example = "2025-07-23 12:30:30")
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	@Schema(description = "프로젝트에 속한 모든 파일/디렉토리 목록")
	private List<File> files = new ArrayList<>();

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	@Schema(description = "프로젝트 멤버 목록")
	private List<ProjectMember> members = new ArrayList<>();

	@Schema(hidden = true)
	public void updateInfo(String newName, String newDescription) {
		if (newName != null) {
			this.name = newName;
		}
		if (newDescription != null) {
			this.description = newDescription;
		}
	}

	@Schema(hidden = true)
	public boolean isOwner(Long userId) {
		return this.ownerId.equals(userId);
	}

	@Schema(hidden = true)
	public boolean hasMember(Long userId) {
		return this.isPublic || this.ownerId.equals(userId) ||
			this.members.stream().anyMatch(member -> member.getUserId().equals(userId));
	}

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	@Schema(description = "프로젝트에 보낸 초대장 목록")
	private List<ProjectInvitation> invitations = new ArrayList<>();

}
