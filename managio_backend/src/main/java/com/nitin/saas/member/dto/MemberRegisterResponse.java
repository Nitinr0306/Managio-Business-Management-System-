package com.nitin.saas.member.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberRegisterResponse {
    private boolean requiresVerification;
    private String email;
}