package com.nitin.saas.member.service;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.entity.BusinessMembership;
import com.nitin.saas.business.enums.BusinessRole;
import com.nitin.saas.business.repository.BusinessMembershipRepository;
import com.nitin.saas.business.service.BusinessAuthorizationService;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final BusinessAuthorizationService authorizationService;

    public MemberService(
            MemberRepository memberRepository,
            BusinessMembershipRepository membershipRepository,
            BusinessAuthorizationService authorizationService
    ) {
        this.memberRepository = memberRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
    }



    public Member addMember(
            Business business,
            User requester,
            User memberUser,
            String name,
            String phone
    ) {

        BusinessMembership membership =
                membershipRepository.findByBusinessAndUser(business, requester)
                        .orElseThrow(() ->
                                new IllegalStateException("Not part of business")
                        );


        if (membership.getRole() != BusinessRole.OWNER &&
                membership.getRole() != BusinessRole.STAFF) {
            throw new IllegalStateException("Not allowed to add members");
        }


        if (memberRepository.existsByBusinessAndPhone(business, phone)) {
            throw new IllegalStateException("Member already exists");
        }


        Member member = new Member(
                memberUser,
                business,
                name,
                phone
        );

        return memberRepository.save(member);
    }


    public Member updateMember(
            Business business,
            User requester,
            Long memberId,
            String name,
            String phone
    ) {
        authorizationService.authorizeOwnerOrStaff(business, requester);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Member not found")
                );

        member.updateDetails(name, phone);
        return memberRepository.save(member);
    }


    public void deactivateMember(
            Business business,
            User requester,
            Long memberId
    ) {
        authorizationService.authorizeOwnerOrStaff(business, requester);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Member not found")
                );

        member.deactivate();
        memberRepository.save(member);
    }
}
