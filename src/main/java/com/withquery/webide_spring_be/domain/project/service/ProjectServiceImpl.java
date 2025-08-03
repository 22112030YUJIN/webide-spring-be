package com.withquery.webide_spring_be.domain.project.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.withquery.webide_spring_be.domain.collaboration.repository.ProjectInvitationRepository;
import com.withquery.webide_spring_be.domain.collaboration.service.ProjectMemberService;
import com.withquery.webide_spring_be.domain.file.dto.FileTreeNode;
import com.withquery.webide_spring_be.domain.file.service.FileService;
import com.withquery.webide_spring_be.domain.project.dto.ProjectCreateRequest;
import com.withquery.webide_spring_be.domain.project.dto.ProjectDetailResponse;
import com.withquery.webide_spring_be.domain.project.dto.ProjectResponse;
import com.withquery.webide_spring_be.domain.project.dto.ProjectUpdateRequest;
import com.withquery.webide_spring_be.domain.project.entity.Project;
import com.withquery.webide_spring_be.domain.collaboration.entity.ProjectMember;
import com.withquery.webide_spring_be.domain.collaboration.entity.ProjectMemberRole;
import com.withquery.webide_spring_be.domain.collaboration.repository.ProjectMemberRepository;
import com.withquery.webide_spring_be.domain.project.repository.ProjectRepository;
import com.withquery.webide_spring_be.domain.user.entity.User;
import com.withquery.webide_spring_be.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectMemberService projectMemberService;
	private final FileService fileService;
	private final UserRepository userRepository;
	private final ProjectInvitationRepository projectInvitationRepository;

	@Override
	public ProjectResponse createProject(ProjectCreateRequest request, String userEmail) {
		if (request.getName() == null || request.getName().trim().isEmpty()) {
			throw new IllegalArgumentException("프로젝트 이름은 필수입니다.");
		}

		User creator = userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new IllegalArgumentException("생성자를 찾을 수 없습니다."));
		Long userId = creator.getId();

		Project project = Project.builder()
			.name(request.getName())
			.description(request.getDescription())
			.ownerId(userId)
			.isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
			.build();
		Project savedProject = projectRepository.save(project);

		projectMemberService.addOwnerMember(savedProject.getId(), userEmail);

		return ProjectResponse.from(savedProject, "프로젝트가 성공적으로 생성되었습니다.");
	}

	@Override
	public List<ProjectResponse> getMyProjects(String userEmail) {
		User user = userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
		Long userId = user.getId();

		List<ProjectMember> members = projectMemberRepository.findByUserIdAndIsActiveTrue(userId);

		return members.stream()
			.map(member -> ProjectResponse.from(member.getProject(), member.getRole()))
			.collect(Collectors.toList());
	}

	@Override
	public ProjectDetailResponse getProjectDetail(Long projectId, String userEmail) {
		User user = userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
		Long userId = user.getId();

		Project project = getProject(projectId);

		ProjectMemberRole userRole = projectMemberService.getMemberRole(projectId, userEmail)
			.orElseThrow(() -> new RuntimeException("프로젝트 접근 권한이 없습니다."));

		FileTreeNode fileTree = fileService.getFileTree(projectId);

		return ProjectDetailResponse.from(
			ProjectResponse.from(project, userRole), fileTree);

	}

	@Override
	public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request, String userEmail) {
		if (request.getNewName() == null || request.getNewName().trim().isEmpty()) {
			throw new IllegalArgumentException("프로젝트 이름은 공백일 수 없습니다.");
		}

		Project project = getProjectWithPermissionCheck(projectId, userEmail, ProjectMemberRole.OWNER,
			ProjectMemberRole.MEMBER);

		project.updateInfo(request.getNewName(), request.getNewDescription());

		return ProjectResponse.from(project, "프로젝트가 성공적으로 수정되었습니다.");

	}

	@Override
	public void deleteProject(Long projectId, String userEmail) {
		Project project = getProjectWithPermissionCheck(projectId, userEmail, ProjectMemberRole.OWNER);

		projectInvitationRepository.deleteByProjectId(projectId);
		fileService.deleteProjectFilesRecursively(projectId);
		projectMemberService.deleteProjectMembersByProjectId(projectId);
		projectRepository.delete(project);

	}

	@Override
	public boolean existsProject(Long projectId) {
		return projectRepository.existsById(projectId);
	}

	@Override
	public Project getProject(Long projectId) {
		return projectRepository.findById(projectId)
			.orElseThrow();
	}

	private Project getProjectWithPermissionCheck(Long projectId, String userEmail,
		ProjectMemberRole... requiredRoles) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다."));

		if (!projectMemberService.hasPermission(projectId, userEmail, requiredRoles)) {
			throw new RuntimeException("프로젝트를 찾을 수 없습니다.");
		}

		return project;
	}
}
