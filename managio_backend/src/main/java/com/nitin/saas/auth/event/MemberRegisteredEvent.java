package com.nitin.saas.auth.event;

import com.nitin.saas.member.entity.Member;

public class MemberRegisteredEvent {
    private final Member member;
    private final String token;

    public MemberRegisteredEvent(Member member, String token) {
        this.member = member;
        this.token = token;
    }

    public Member getMember() { return member; }
    public String getToken() { return token; }
}