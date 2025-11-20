package com.project.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import com.project.app.user.entity.User;
import com.project.app.user.repository.UserRepository;

/**
 * Spring Boot 애플리케이션의 시작점 (메인 클래스)
 * 
 * 특정 기능을 제외하고 싶을 때 아래 주석을 해제하세요:
 * - Batch 기능 제외: @SpringBootApplication(exclude = {BatchAutoConfiguration.class})
 * - JPA 기능 제외: @SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class})
 * - MyBatis 기능 제외: @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
 */
@SpringBootApplication
public class AppApplication {

	// 애플리케이션 실행 메소드
	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}

	/**
	 * RestTemplate Bean 등록
	 * 외부 API를 호출할 때 사용하는 HTTP 클라이언트 도구
	 */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	/**
	 * 애플리케이션 시작 시 테스트용 사용자 계정 자동 생성
	 * 개발/테스트 환경에서 편리하게 사용하기 위한 기능
	 * 운영 환경에서는 이 메소드를 제거하거나 비활성화하세요!
	 */
	@Bean
	public CommandLineRunner createTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// 일반 사용자 계정 생성 (아이디: user, 비밀번호: user)
			if (!userRepository.existsByUserId("user")) {
				User testUser = new User();
				testUser.setUserId("user");
				testUser.setUserName("일반");
				// 비밀번호를 암호화하여 저장 (보안을 위해 평문 저장 금지!)
				testUser.setPassword(passwordEncoder.encode("user"));
				testUser.setAuth("USER"); // 일반 사용자 권한
				userRepository.save(testUser);
				System.out.println("[테스트 계정 생성] 아이디: user, 비밀번호: user");
			}

			// 관리자 계정 생성 (아이디: admin, 비밀번호: admin)
			if (!userRepository.existsByUserId("admin")) {
				User adminUser = new User();
				adminUser.setUserId("admin");
				adminUser.setUserName("관리자");
				adminUser.setPassword(passwordEncoder.encode("admin"));
				adminUser.setAuth("ADMIN"); // 관리자 권한
				userRepository.save(adminUser);
				System.out.println("[테스트 계정 생성] 아이디: admin, 비밀번호: admin");
			}
		};
	}

}
