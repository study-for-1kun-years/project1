package com.example.demo3.user_sys.service;

import com.example.demo3.user_sys.entity.User;
import com.example.demo3.user_sys.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

//        // 添加日志，输出加载的密码 登录时查询调试
//        System.out.println("Loaded password for username " + username + ": " + user.getPassword());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus().equals("on"),
                true, true, true,
                Collections.singletonList(() -> "ROLE_" + user.getRole())
        );
    }
}

