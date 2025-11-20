package com.project.app.config.security;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT (JSON Web Token) 토큰 처리 클래스
 * 로그인 시 토큰을 생성하고, 요청 시 토큰을 검증하는 역할을 합니다
 * 
 * JWT란? 서버와 클라이언트 간에 정보를 안전하게 전달하기 위한 토큰 기반 인증 방식
 */
@Slf4j
@Component
public class JwtTokenProvider {

	// application.properties에서 jwt.secret 값을 가져옵니다
	// 이 값은 토큰을 암호화하는 비밀 키로 사용됩니다
    @Value("${jwt.secret}")
    private String secretKey;
    
    // 토큰 유효 시간: 30분 (30분 * 60초 * 1000밀리초)
    private final long tokenValidTime = 30 * 60 * 1000L;
    
    // 사용자 정보를 로드하는 서비스
    private final UserDetailsService userDetailsService;
    
    // 생성자: UserDetailsService를 주입받습니다
    public JwtTokenProvider(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    /**
     * 초기화 메소드: 빈 생성 후 자동 실행
     * 비밀 키를 Base64로 인코딩하여 보안성을 높입니다
     */
    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }
    
    /**
     * JWT 토큰 생성 메소드
     * 로그인 성공 시 호출되어 사용자에게 발급할 토큰을 만듭니다
     * 
     * @param userPk 사용자 아이디
     * @param roles 사용자 권한 목록 (USER, ADMIN 등)
     * @return 생성된 JWT 토큰 문자열
     */
    @SuppressWarnings("deprecation")
	public String createToken(String userPk, List<String> roles) {
		// 토큰에 담을 정보 설정
        Claims claims = Jwts.claims().setSubject(userPk);
        claims.put("roles", roles); // 권한 정보 추가
        Date now = new Date();
        
        return Jwts.builder()
                .setClaims(claims)                                          // 사용자 정보 저장
                .setIssuedAt(now)                                           // 토큰 발행 시간
                .setExpiration(new Date(now.getTime() + tokenValidTime))   // 토큰 만료 시간
                .signWith(SignatureAlgorithm.HS256, secretKey)             // HS256 알고리즘으로 서명
                .compact(); // 토큰 생성
    }
    
    /**
     * JWT 토큰에서 인증 정보 추출
     * 토큰을 해석하여 Spring Security가 이해할 수 있는 인증 객체로 변환합니다
     * 
     * @param token JWT 토큰
     * @return Spring Security 인증 객체
     */
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserPk(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
    
    /**
     * 토큰에서 사용자 아이디 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 아이디
     */
    @SuppressWarnings("deprecation")
	public String getUserPk(String token) {
        return Jwts.parser()
        		.setSigningKey(secretKey)      // 비밀 키로 토큰 검증
        		.parseClaimsJws(token)         // 토큰 파싱
        		.getBody()
        		.getSubject();                 // 사용자 아이디 반환
    }
    
    /**
     * 토큰 유효성 검증
     * 토큰이 올바른지, 만료되지 않았는지 확인합니다
     * 
     * @param jwtToken 검증할 JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    @SuppressWarnings("deprecation")
	public boolean validateToken(String jwtToken) {
        try {
            Jws<Claims> claims = Jwts.parser()
            		.setSigningKey(secretKey)
            		.parseClaimsJws(jwtToken);
            // 토큰의 만료 시간이 현재 시간보다 이후인지 확인
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            // 토큰이 잘못되었거나 만료된 경우
            return false;
        }
    }
}