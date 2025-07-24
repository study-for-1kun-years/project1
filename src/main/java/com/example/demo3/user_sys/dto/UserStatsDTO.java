package com.example.demo3.user_sys.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserStatsDTO {
    private int totalUsers;
    private int monthlyIncrease;
    private List<String> dates;
    private List<Long> counts;

    public UserStatsDTO(int totalUsers, int monthlyIncrease, List<String> dates, List<Long> counts) {
        this.totalUsers = totalUsers;
        this.monthlyIncrease = monthlyIncrease;
        this.dates = dates;
        this.counts = counts;
    }
}
