package com.dochiri.nplusguard.sample.service;

import com.dochiri.nplusguard.sample.domain.Member;
import com.dochiri.nplusguard.sample.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public MemberQueryService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public int countOrdersWithNPlusOne() {
        List<Member> members = memberRepository.findAll();
        return members.stream()
                .mapToInt(member -> member.getOrders().size())
                .sum();
    }

    public int countOrdersWithFetchJoin() {
        List<Member> members = memberRepository.findAllWithOrders();
        return members.stream()
                .mapToInt(member -> member.getOrders().size())
                .sum();
    }
}
