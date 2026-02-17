package com.nitin.saas.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskReminder {
    private String task;
    private Integer count;
    private String priority;
}