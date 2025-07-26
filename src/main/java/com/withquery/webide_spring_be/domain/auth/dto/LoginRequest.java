package com.withquery.webide_spring_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 요청")
public record LoginRequest(
    @Schema(description = "이메일", example = "user@example.com", required = true)
    String email,
    
    @Schema(description = "비밀번호", example = "password123", required = true)
    String password
) {} 