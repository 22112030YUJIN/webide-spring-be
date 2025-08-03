package com.withquery.webide_spring_be.domain.user.service;

import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.withquery.webide_spring_be.domain.user.dto.PasswordChangeRequest;
import com.withquery.webide_spring_be.domain.user.dto.PasswordChangeResponse;
import com.withquery.webide_spring_be.domain.user.dto.UserDeleteResponse;
import com.withquery.webide_spring_be.domain.user.dto.UserRegistrationRequest;
import com.withquery.webide_spring_be.domain.user.dto.UserRegistrationResponse;
import com.withquery.webide_spring_be.domain.user.dto.UserUpdateRequest;
import com.withquery.webide_spring_be.domain.user.dto.UserUpdateResponse;
import com.withquery.webide_spring_be.domain.user.entity.User;
import com.withquery.webide_spring_be.domain.user.repository.UserRepository;
import com.withquery.webide_spring_be.util.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new RuntimeException("이미 존재하는 이메일입니다.");
		}

		if (userRepository.existsByNickname(request.nickname())) {
			throw new RuntimeException("이미 존재하는 닉네임입니다.");
		}

		User user = new User();
		user.setEmail(request.email());
		user.setNickname(request.nickname());
		user.setPassword(passwordEncoder.encode(request.password()));

		User savedUser = userRepository.save(user);
		return new UserRegistrationResponse("회원가입이 완료되었습니다.", savedUser.getId());
	}

	@Override
	public UserUpdateResponse updateUser(String email, UserUpdateRequest request) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

		if (Objects.equals(user.getNickname(), request.nickname())) {

			String token = jwtTokenProvider.generateToken(user.getEmail(), user.getNickname(), user.getId());
			return new UserUpdateResponse(user.getNickname(), token, "닉네임이 변경되지 않았습니다.");
		}

		if (userRepository.existsByNickname(request.nickname())) {
			throw new RuntimeException("이미 존재하는 닉네임입니다.");
		}

		user.setNickname(request.nickname());
		userRepository.save(user);

		String newToken = jwtTokenProvider.generateToken(user.getEmail(), user.getNickname(), user.getId());

		return new UserUpdateResponse(user.getNickname(), newToken, "사용자 정보가 수정되었습니다.");
	}

	@Override
	public UserDeleteResponse deleteUser(String email) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

		userRepository.delete(user);

		return new UserDeleteResponse("회원 탈퇴가 완료되었습니다.");
	}

	@Override
	public PasswordChangeResponse changePassword(String email, PasswordChangeRequest request) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
		}

		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);

		return new PasswordChangeResponse("비밀번호가 성공적으로 변경되었습니다.");
	}
} 