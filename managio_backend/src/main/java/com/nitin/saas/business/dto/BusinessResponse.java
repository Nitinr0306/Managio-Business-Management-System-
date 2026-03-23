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
    private String publicId;
    private Long ownerId;
    private String ownerPublicId;
    private String name;

    // FIX 11: these were missing from the response DTO.
    // Without them the frontend receives null for type/description/city/state/country
    // even if the entity is fixed — the builder simply never sets them.
    private String type;
    private String description;
    private String address;
    private String city;
    private String state;
    private String country;

    private String phone;
    private String email;
    private String status;
    private Integer memberCount;
    private Integer staffCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}