package com.nitin.saas.dashboard.controller;

import com.nitin.saas.common.security.UserPrincipal;
import com.nitin.saas.dashboard.dto.MemberDashboardResponse;
import com.nitin.saas.dashboard.service.MemberDashboardService;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/member")
public class MemberDashboardController {

    private final MemberDashboardService service;
    private final MemberRepository memberRepository;

    public MemberDashboardController(MemberDashboardService service, MemberRepository memberRepository) {
        this.service = service;
        this.memberRepository = memberRepository;
    }

    @GetMapping
    public MemberDashboardResponse dashboard(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Member member = memberRepository
                .findByUserId(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Member not found"));

        return service.getDashboard(member);
    }
}
