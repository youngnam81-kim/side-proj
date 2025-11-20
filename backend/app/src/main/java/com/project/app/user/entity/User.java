package com.project.app.user.entity; // 패키지 변경

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data  // @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor 포함
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "APP_USER")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "userId", nullable = false, unique = true)
    private String userId;
    
    @Column(name = "userName", nullable = false)
    private String userName;
    
    @Column(nullable = false)
    private String password;
    
    @Column
    private String auth;
    
    public User(String userId, String password, String userName) {
        this.userId = userId;
        this.password = password;
        this.userName = userName;
    }
}