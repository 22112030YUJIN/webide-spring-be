package com.withquery.webide_spring_be.domain.collaboration.service;

import java.util.List;
import java.util.Optional;

import com.withquery.webide_spring_be.domain.collaboration.dto.InvitationActionRequest;
import com.withquery.webide_spring_be.domain.collaboration.dto.InvitationResponse;
import com.withquery.webide_spring_be.domain.collaboration.dto.InviteMemberRequest;
import com.withquery.webide_spring_be.domain.collaboration.dto.ProjectMemberResponse;
import com.withquery.webide_spring_be.domain.collaboration.entity.ProjectMemberRole;
import com.withquery.webide_spring_be.domain.user.entity.User;

public interface ProjectMemberService {

	User getUserByEmail(String userEmail);

	void addOwnerMember(Long projectId, String userEmail);

	Optional<ProjectMemberRole> getMemberRole(Long projectId, String userEmail);

	boolean isMember(Long projectId, String userEmail);

	boolean hasPermission(Long projectId, String userEmail, ProjectMemberRole... requiredRoles);

	void inviteMember(Long projectId, String inviterEmail, InviteMemberRequest request);

	List<ProjectMemberResponse> getProjectMembers(Long projectId, String userEmail);

	List<InvitationResponse> getUserInvitations(String userEmail);

	List<InvitationResponse> getSentInvitations(String senderEmail);

	void handleInvitation(Long invitationId, String userEmail, InvitationActionRequest request);

	void leaveProject(Long projectId, String userEmail);

	void deleteProjectMembersByProjectId(Long projectId);
}
