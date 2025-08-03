package com.withquery.webide_spring_be.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.withquery.webide_spring_be.common.dto.ErrorResponse;
import com.withquery.webide_spring_be.domain.user.dto.PasswordChangeRequest;
import com.withquery.webide_spring_be.domain.user.dto.PasswordChangeResponse;
import com.withquery.webide_spring_be.domain.user.dto.UserDeleteResponse;
import com.withquery.webide_spring_be.domain.user.dto.UserInfoResponse;
import com.withquery.webide_spring_be.domain.user.dto.UserRegistrationRequest;
import com.withquery.webide_spring_be.domain.user.dto.UserRegistrationResponse;
import com.withquery.webide_spring_be.domain.user.dto.UserUpdateRequest;
import com.withquery.webide_spring_be.domain.user.dto.UserUpdateResponse;
import com.withquery.webide_spring_be.domain.user.service.UserService;
import com.withquery.webide_spring_be.util.jwt.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

	private final UserService userService;

	@PostMapping("/register")
	@Operation(
		summary = "회원가입",
		description = "새로운 사용자를 등록합니다. 이메일과 닉네임은 중복될 수 없습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "201",
			description = "회원가입 성공",
			content = @Content(
				schema = @Schema(implementation = UserRegistrationResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (중복된 이메일 또는 닉네임, 비밀번호 정책 위배)",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)
			)
		)
	})
	@SecurityRequirement(name = "none")
	public ResponseEntity<UserRegistrationResponse> registerUser(
		@Valid @RequestBody UserRegistrationRequest request) {
		UserRegistrationResponse response = userService.registerUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/me")
	@Operation(
		summary = "내 정보 조회",
		description = "현재 로그인한 사용자의 정보를 조회합니다. 로그인 상태 체크를 겸합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "사용자 정보 조회 성공",
			content = @Content(
				schema = @Schema(implementation = UserInfoResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 요청",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)
			)
		)
	})
	public ResponseEntity<UserInfoResponse> getMyInfo(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails)authentication.getPrincipal();
		return ResponseEntity.ok(new UserInfoResponse(userDetails.getId(), userDetails.getEmail(), userDetails.getNickname()));
	}

	@PutMapping("/me")
	@Operation(
		summary = "내 정보 수정",
		description = "현재 로그인한 사용자의 닉네임을 수정합니다. 닉네임은 중복될 수 없습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "사용자 정보 수정 성공",
			content = @Content(
				schema = @Schema(implementation = UserUpdateResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (중복된 닉네임)",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 요청",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)
			)
		)
	})
	public ResponseEntity<UserUpdateResponse> updateMyInfo(
		Authentication authentication,
		@RequestBody UserUpdateRequest request) {
		CustomUserDetails userDetails = (CustomUserDetails)authentication.getPrincipal();
		UserUpdateResponse response = userService.updateUser(userDetails.getEmail(), request);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/password")
	@Operation(
		summary = "비밀번호 변경",
		description = "현재 로그인한 사용자의 비밀번호를 변경합니다. 현재 비밀번호 확인이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "비밀번호 변경 성공",
			content = @Content(
				schema = @Schema(implementation = PasswordChangeResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (현재 비밀번호 불일치)",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 요청",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)
			)
		)
	})
	public ResponseEntity<PasswordChangeResponse> changePassword(
		Authentication authentication,
		@RequestBody PasswordChangeRequest request) {
		CustomUserDetails userDetails = (CustomUserDetails)authentication.getPrincipal();
		PasswordChangeResponse response = userService.changePassword(userDetails.getEmail(), request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/me")
	@Operation(
		summary = "회원 탈퇴",
		description = "현재 로그인한 사용자의 계정을 삭제합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "회원 탈퇴 성공",
			content = @Content(
				schema = @Schema(implementation = UserDeleteResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 요청",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)
			)
		)
	})
	public ResponseEntity<UserDeleteResponse> deleteMyAccount(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails)authentication.getPrincipal();
		UserDeleteResponse response = userService.deleteUser(userDetails.getEmail());
		return ResponseEntity.ok(response);
	}
} 