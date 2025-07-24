// com.example.demo3.config.security.SessionController.java
package com.example.demo3.config.security;

import com.example.demo3.ApiResponse.ApiResponse;
import com.example.demo3.config.redis.RedisSessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    @Autowired
    private RedisSessionRegistry redisSessionRegistry;

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCount() {
        return ResponseEntity.ok(ApiResponse.success(redisSessionRegistry.getSessionCount()));
    }

    @GetMapping("/details")
    public ResponseEntity<ApiResponse<List<String>>> getDetails() {
        return ResponseEntity.ok(ApiResponse.success(redisSessionRegistry.listActiveSessions()));
    }
}
