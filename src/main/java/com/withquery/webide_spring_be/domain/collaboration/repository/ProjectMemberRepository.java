package com.withquery.webide_spring_be.domain.collaboration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.withquery.webide_spring_be.domain.collaboration.entity.ProjectMember;
import com.withquery.webide_spring_be.domain.collaboration.entity.ProjectMemberRole;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	List<ProjectMember> findByUserIdAndIsActiveTrue(Long userId);

	Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

	boolean existsByProjectIdAndUserIdAndIsActiveTrue(Long projectId, Long userId);

	List<ProjectMember> findByProjectIdAndIsActiveTrue(Long projectId);

	List<ProjectMember> findByProjectIdAndRole(Long projectId, String role);

	long countByProjectIdAndIsActiveTrue(Long projectId);

	@Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.project WHERE pm.userId = :userId AND pm.isActive = true")
	List<ProjectMember> findActiveProjectsByUserId(@Param("userId") Long userId);

	@Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.role = :role AND pm.isActive = true")
	List<ProjectMember> findActiveMembersByProjectIdAndRole(@Param("projectId") Long projectId, @Param("role")
	ProjectMemberRole role);

	List<ProjectMember> findByProjectId(Long projectId);
}