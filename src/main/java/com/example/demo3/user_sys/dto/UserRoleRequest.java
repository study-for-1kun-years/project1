package com.example.demo3.user_sys.dto;

import lombok.Data;

@Data
public class UserRoleRequest {
    private Long userId;
    private String role;
}
