package com.example.demo3.user_sys.dto;

import lombok.Data;

@Data
public class UserStatusRequest {
    private Long userId;
    private String status;
}
