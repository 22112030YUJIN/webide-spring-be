package com.withquery.webide_spring_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 응답")
public record UserInfoResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long id,

    @Schema(description = "이메일", example = "user@example.com")
    String email,
    
    @Schema(description = "닉네임", example = "username")
    String nickname
) {} 