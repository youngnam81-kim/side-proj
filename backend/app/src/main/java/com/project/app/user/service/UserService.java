package com.project.app.user.service; // 패키지 변경

import java.util.Optional;

import com.project.app.user.dto.UserRequestDto;
import com.project.app.user.entity.User;

public interface UserService {
	
	User createUser(UserRequestDto userRequestDto);
	boolean existsByUserId(String userId);
	Optional<User> findByUserId(String userId);
}