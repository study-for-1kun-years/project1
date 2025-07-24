package com.example.demo3.user_sys.controller;

import com.example.demo3.ApiResponse.ApiResponse;
import com.example.demo3.user_sys.dto.*;
import com.example.demo3.user_sys.entity.User;
import com.example.demo3.user_sys.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SessionRegistry sessionRegistry;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody User user) {
        if (userService.usernameExists(user.getUsername())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名已存在"));
        }
        if (userService.emailExists(user.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("邮箱已注册"));
        }

        user.setCreate_time(LocalDateTime.now());
        userService.saveUser(user);
        return ResponseEntity.ok(ApiResponse.success("注册成功"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<User>> login(@RequestBody User user, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );

            // 创建 SecurityContext 并设置认证信息
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 🔑 手动存入 Session
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);
            System.out.println("Login Session ID: " + session.getId());

            // ✅ 注册 Session 到 SessionRegistry
            sessionRegistry.registerNewSession(session.getId(), authentication.getPrincipal());

            Optional<User> foundUser = userService.getUserByUsername(user.getUsername());
            if (foundUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("用户不存在"));
            }

            User loggedInUser = foundUser.get();
            if ("off".equals(loggedInUser.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("账户已被禁用"));
            }

            userService.updateLoginInfo(loggedInUser.getUsername(), request);
            return ResponseEntity.ok(ApiResponse.success(loggedInUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("用户名或密码错误"));
        }
    }



    @PostMapping("/admin-login")
    public ResponseEntity<ApiResponse<User>> adminLogin(@RequestBody User user, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Optional<User> foundUser = userService.getUserByUsername(user.getUsername());
            if (foundUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("用户不存在"));
            }

            User loggedInUser = foundUser.get();
            if ("off".equals(loggedInUser.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("账户已被禁用"));
            }
            if (!"ADMIN".equals(loggedInUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("非管理员用户，无权访问"));
            }

            userService.updateLoginInfo(loggedInUser.getUsername(), request);
            return ResponseEntity.ok(ApiResponse.success(loggedInUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("用户名或密码错误"));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current authentication: " + (auth != null ? auth.getName() : "null"));
        String username = auth != null ? auth.getName() : null;
        Optional<User> user = userService.getUserByUsername(username);
        return user.isPresent()
                ? ResponseEntity.ok(ApiResponse.success(user.get()))
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("用户不存在"));
    }


    @PutMapping("/me")
    public ResponseEntity<ApiResponse<String>> updateCurrentUser(@RequestBody User updatedUser) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOptional = userService.getUserByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("用户不存在"));
        }

        User user = userOptional.get();
        if (updatedUser.getNickname() != null) user.setNickname(updatedUser.getNickname());
        if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
        if (updatedUser.getUser_bio() != null) user.setUser_bio(updatedUser.getUser_bio());
        userService.saveUser(user);

        return ResponseEntity.ok(ApiResponse.success("个人信息更新成功"));
    }

    @PostMapping("/me/password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOptional = userService.getUserByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("用户不存在"));
        }

        userService.changePassword(userOptional.get().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<String>> updateUser(@RequestBody User updatedUser) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> currentUser = userService.getUserByUsername(username);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("当前用户不存在"));
        }

        if (!"ADMIN".equals(currentUser.get().getRole()) && !updatedUser.getId().equals(currentUser.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("无权修改其他用户信息"));
        }

        Optional<User> userOptional = userService.getUserById(updatedUser.getId());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("用户不存在"));
        }

        User user = userOptional.get();
        if (updatedUser.getNickname() != null) user.setNickname(updatedUser.getNickname());
        if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
        if (updatedUser.getUser_bio() != null) user.setUser_bio(updatedUser.getUser_bio());
        if (updatedUser.getStatus() != null && "ADMIN".equals(currentUser.get().getRole())) {
            user.setStatus(updatedUser.getStatus());
        }
        userService.saveUser(user);

        return ResponseEntity.ok(ApiResponse.success("用户信息更新成功"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        Optional<User> userOptional = userService.getUserById(id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("用户不存在"));
        }
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("用户删除成功"));
    }

    @PostMapping("/set-role")
    public ResponseEntity<ApiResponse<String>> setUserRole(@RequestBody UserRoleRequest request) {
        try {
            userService.setUserRole(request.getUserId(), request.getRole());
            return ResponseEntity.ok(ApiResponse.success("角色设置成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/set-status")
    public ResponseEntity<ApiResponse<String>> setUserStatus(@RequestBody UserStatusRequest request) {
        try {
            userService.setUserStatus(request.getUserId(), request.getStatus());
            return ResponseEntity.ok(ApiResponse.success("状态设置成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("注销成功"));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<String>> resetUserPassword(@RequestBody UserIdRequest request) {
        try {
            Optional<User> userOptional = userService.getUserById(request.getUserId());
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("用户不存在"));
            }
            User user = userOptional.get();
            user.setPassword("test1234");
            userService.saveUser(user);
            return ResponseEntity.ok(ApiResponse.success("用户密码已重置为 test1234"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("重置密码失败: " + e.getMessage()));
        }
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOptional = userService.getUserByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("用户不存在"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("上传的文件为空"));
        }

        try {
            String uploadDir = System.getProperty("user.dir") + File.separator + "user_source" + File.separator + "user_avatar" + File.separator;
            Files.createDirectories(Paths.get(uploadDir));

            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            Path filePath = Paths.get(uploadDir, uniqueFileName);
            Files.write(filePath, file.getBytes());

            User user = userOptional.get();
            String avatarUrl = "http://localhost:8080/user_source/user_avatar/" + uniqueFileName;
            user.setUser_avater(avatarUrl);
            userService.saveUser(user);

            return ResponseEntity.ok(ApiResponse.success(avatarUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("头像上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/me/avatar")
    public ResponseEntity<ApiResponse<String>> getAvatarUrl() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOptional = userService.getUserByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("用户不存在"));
        }

        User user = userOptional.get();
        String avatarUrl = user.getUser_avater() != null ? user.getUser_avater() : "http://localhost:8080/1.png";
        return ResponseEntity.ok(ApiResponse.success(avatarUrl));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStatsDTO>> getUserStats() {
        UserStatsDTO stats = userService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
