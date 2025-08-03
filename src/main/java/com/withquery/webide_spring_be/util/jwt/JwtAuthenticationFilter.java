package com.withquery.webide_spring_be.util.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.withquery.webide_spring_be.common.dto.ErrorResponse;
import com.withquery.webide_spring_be.config.PublicEndpoints;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isPublicEndpoint(uri)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String token = getTokenFromRequest(request);
            if (StringUtils.hasText(token)) {
                if (jwtTokenProvider.validateToken(token)) {
                    Long id = jwtTokenProvider.getIdFromToken(token);
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    String nickname = jwtTokenProvider.getNicknameFromToken(token);
                    UserDetails userDetails = new CustomUserDetails(id, email, nickname);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 토큰 검증 성공: {}", email);
                } else {
                    log.warn("유효하지 않은 JWT 토큰");
                    SecurityContextHolder.clearContext();
                    sendErrorResponse(response, "INVALID_TOKEN", "유효하지 않은 JWT 토큰입니다.");
                    return;
                }
            } else {
                log.warn("JWT 토큰이 없습니다");
                SecurityContextHolder.clearContext();
                sendErrorResponse(response, "UNAUTHORIZED", "인증되지 않은 요청입니다.");
                return;
            }
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        for (String pattern : PublicEndpoints.ENDPOINTS) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private void sendErrorResponse(HttpServletResponse response, String code, String message) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(code, message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (JwtTokenExtractor.isValidAuthHeader(bearerToken)) {
            return JwtTokenExtractor.extractToken(bearerToken);
        }
        return null;
    }
} 