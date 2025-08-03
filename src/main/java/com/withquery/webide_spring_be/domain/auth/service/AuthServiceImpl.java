package com.withquery.webide_spring_be.domain.auth.service;

import com.withquery.webide_spring_be.util.jwt.JwtTokenProvider;
import com.withquery.webide_spring_be.domain.auth.dto.LoginRequest;
import com.withquery.webide_spring_be.domain.auth.dto.LoginResponse;
import com.withquery.webide_spring_be.domain.auth.dto.PasswordResetRequestRequest;
import com.withquery.webide_spring_be.domain.auth.dto.PasswordResetRequestResponse;
import com.withquery.webide_spring_be.domain.auth.dto.PasswordResetConfirmRequest;
import com.withquery.webide_spring_be.domain.auth.dto.PasswordResetConfirmResponse;
import com.withquery.webide_spring_be.domain.user.entity.User;
import com.withquery.webide_spring_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        String passwordHash = (user != null) ? user.getPassword() : "$2a$10$7EqJtq98hPqEX7fNZaFWoOeIx8eD1Zzt1aG6zV4bE4xF2z5i5f7eK"; // bcrypt hash of "dummy"
        if (!passwordEncoder.matches(request.password(), passwordHash)) {
            throw new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다.");
        }
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getNickname(), user.getId());

        return new LoginResponse("로그인이 성공했습니다.", token, user.getNickname());
    }

    @Override
    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequestRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다."));

        String resetToken = jwtTokenProvider.generatePasswordResetToken(user.getEmail());

        return new PasswordResetRequestResponse(resetToken, "비밀번호 재설정 토큰이 발급되었습니다.");
    }

    @Override
    public PasswordResetConfirmResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (!jwtTokenProvider.validatePasswordResetToken(request.resetToken())) {
            throw new RuntimeException("유효하지 않거나 만료된 토큰입니다.");
        }

        String email = jwtTokenProvider.getEmailFromPasswordResetToken(request.resetToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return new PasswordResetConfirmResponse("비밀번호가 성공적으로 변경되었습니다.");
    }
} 