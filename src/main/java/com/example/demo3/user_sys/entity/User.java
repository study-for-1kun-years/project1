package com.example.demo3.user_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    private String ip;

    @Column(nullable = false, columnDefinition = "VARCHAR(10) DEFAULT 'on'")
    private String status = "on";

    @CreationTimestamp
    private LocalDateTime login_time;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime create_time;

    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'http://localhost:8080/1.png'")
    private String user_avater = "http://localhost:8080/1.png";

    @Column(columnDefinition = "TEXT DEFAULT '这个人很懒，还没有写简介...'")
    private String user_bio = "这个人很懒，还没有写简介...";

    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'USER'")
    private String role = "USER";
}
