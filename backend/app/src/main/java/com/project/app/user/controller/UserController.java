package com.project.app.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.config.security.JwtTokenProvider;
import com.project.app.user.dto.UserRequestDto;
import com.project.app.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
		this.userService = userService;
	}

	@PostMapping("/register")
	@Operation(summary = "회원가입 API", description = "사용자가 회원가입 합니다.")
	public ResponseEntity<?> createUser(@RequestBody UserRequestDto userRequestDto) {
		try {
			if (userRequestDto.getUserId() == null || userRequestDto.getPassword() == null) {
				return ResponseEntity.badRequest().body("아이디와 비밀번호는 필수 항목 입니다.");
			}
			if (userService.existsByUserId(userRequestDto.getUserId())) {
				return ResponseEntity.badRequest().body("이미 사용중인 아이디입니다.");
			}

			userService.createUser(userRequestDto);

			return ResponseEntity.status(HttpStatus.CREATED).body(userRequestDto);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("회원가입 처리 중 오류가 발생했습니다. : " + e.getMessage());
		}
	}

	@GetMapping("/userinfo")
	@Operation(summary = "사용자 userId로 조회", description = "사용자를 조회합니다.")
	public ResponseEntity<?> userinfo(@PathVariable String userId) {
		try {
			userService.findByUserId(userId);

			return ResponseEntity.ok(userService.findByUserId(userId)); // 200 OK와 사용자 정보 반환
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("사용자 정보 조회 처리 중 오류가 발생했습니다. : " + e.getMessage());
		}

	}

}