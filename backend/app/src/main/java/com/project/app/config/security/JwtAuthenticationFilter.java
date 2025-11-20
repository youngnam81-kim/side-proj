package com.project.app.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청을 가로채서 JWT 토큰을 검사하는 필터
 * 
 * 동작 순서:
 * 1. 요청 헤더에서 JWT 토큰 추출
 * 2. 토큰 유효성 검증
 * 3. 유효하면 사용자 인증 정보를 Spring Security에 등록
 * 4. 다음 필터로 요청 전달
 */
@Slf4j
public class JwtAuthenticationFilter extends GenericFilterBean {

	// JWT 토큰을 처리하는 클래스
    private final JwtTokenProvider jwtTokenProvider;
    
    // 생성자: JwtTokenProvider를 주입받습니다
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * 필터 메인 로직
     * 모든 HTTP 요청이 들어올 때마다 실행됩니다
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
    		throws IOException, ServletException {
    	try {
            // 1. HTTP 요청 헤더에서 JWT 토큰 추출
            String token = resolveToken((HttpServletRequest) request);
            
            // 2. 토큰이 존재하고 유효한지 검사
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 3. 토큰에서 사용자 정보를 추출하여 인증 객체 생성
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                
                // 4. Spring Security에 인증 정보 등록 (로그인 상태로 만듬)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            
            // 5. 다음 필터로 요청 전달
            chain.doFilter(request, response);
            
        } catch (IOException e) {
            // 입출력 오류 발생 시
            throw e;
        } catch (ServletException e) {
            // 서블릿 오류 발생 시
            throw e;
        } catch (java.io.IOException e) {
            // IO 예외 처리
            e.printStackTrace();
        } catch (RuntimeException e) {
            // 기타 런타임 오류 (토큰 검증 실패 등)
            throw e;
        }
    }
    
    /**
     * HTTP 요청 헤더에서 JWT 토큰 추출
     * 
     * 헤더 형식: Authorization: Bearer {JWT 토큰}
     * 예시: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * @param request HTTP 요청 객체
     * @return JWT 토큰 문자열 (없으면 null)
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // "Bearer " 로 시작하는지 확인
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // "Bearer " 를 제외한 나머지 부분이 실제 토큰
            return bearerToken.substring(7);
        }
        return null;
    }
}