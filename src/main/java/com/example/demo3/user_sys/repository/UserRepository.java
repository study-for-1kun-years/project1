package com.example.demo3.user_sys.repository;

import com.example.demo3.user_sys.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAll();

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.ip = ?2, u.login_time = ?3 WHERE u.username = ?1")
    void updateLoginInfo(String username, String ip, LocalDateTime loginTime);
}
