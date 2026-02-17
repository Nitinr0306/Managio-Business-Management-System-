package com.nitin.saas.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessResponse {
    private Long id;
    private Long ownerId;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String status;
    private Integer memberCount;
    private Integer staffCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}