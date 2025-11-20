package com.project.app.user.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.config.security.JwtTokenProvider;
import com.project.app.user.dto.LoginRequestDto;
import com.project.app.user.dto.LoginResponseDto;
import com.project.app.user.entity.User;
import com.project.app.user.service.LoginService;
import com.project.app.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 처리 컨트롤러
 * 사용자 로그인 요청을 받아 JWT 토큰을 발급합니다
 */
@Slf4j
@RestController
@RequestMapping(value = {"/api", "/api/auth"})
public class LoginController {

	// 사용자 정보를 처리하는 서비스
	private final UserService userService;
	
	// JWT 토큰을 생성하는 클래스
	private final JwtTokenProvider jwtTokenProvider;
	
	// 비밀번호를 암호화/검증하는 도구
	private PasswordEncoder passwordEncoder;

	// 생성자: 필요한 의존성들을 주입받습니다
	public LoginController(LoginService loginService, UserService userService, 
			JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
		this.userService = userService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 로그인 API
	 * POST /api/login 또는 /api/auth/login
	 * 
	 * 요청 본문 예시:
	 * {
	 *   "userId": "user",
	 *   "password": "user"
	 * }
	 * 
	 * 응답 예시:
	 * {
	 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
	 *   "user": {
	 *     "id": 1,
	 *     "userId": "user",
	 *     "userName": "일반",
	 *     "auth": "USER",
	 *     "success": true,
	 *     "message": "로그인 성공"
	 *   }
	 * }
	 */
	@PostMapping("/login")
	@Operation(summary = "로그인 API", description = "사용자가 로그인 합니다.")
	public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
		// 1. 데이터베이스에서 사용자 조회
		User user = userService.findByUserId(loginRequestDto.getUserId())
				.orElseThrow(() -> new IllegalArgumentException("가입되지 않은 아이디입니다."));
		
		// 2. 비밀번호 확인 (입력한 평문 비밀번호와 DB의 암호화된 비밀번호 비교)
		if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("잘못된 비밀번호입니다.");
		}
		
		// 3. 사용자 권한 정보 설정
		List<String> roles = new ArrayList<>();
		roles.add(user.getAuth()); // USER, ADMIN 등

		// 4. JWT 토큰 생성 (사용자 아이디와 권한 정보 포함)
		String token = jwtTokenProvider.createToken(user.getUserId(), roles);

		// 5. 응답 데이터 구성
		Map<String, Object> response = new HashMap<>();
		response.put("token", token); // 클라이언트에서 저장하여 사용할 토큰
		response.put("user", LoginResponseDto.builder()
				.id(user.getId())
				.userId(user.getUserId())
				.userName(user.getUserName())
				.auth(user.getAuth())
				.success(true)
				.message("로그인 성공")
				.build());

		// 6. HTTP 200 OK와 함께 응답 반환
		return ResponseEntity.ok(response);
	}
}