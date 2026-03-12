package com.dochiri.nplusguard.sample;

import com.dochiri.nplusguard.core.policy.GuardPolicy;
import com.dochiri.nplusguard.core.scope.ThreadLocalGuardScopeManager;
import com.dochiri.nplusguard.sample.domain.Member;
import com.dochiri.nplusguard.sample.domain.PurchaseOrder;
import com.dochiri.nplusguard.sample.repository.MemberRepository;
import com.dochiri.nplusguard.sample.repository.PurchaseOrderRepository;
import com.dochiri.nplusguard.sample.service.MemberQueryService;
import com.dochiri.nplusguard.testsupport.QueryGuardAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NPlusGuardSampleApplicationTests {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private MemberQueryService memberQueryService;

    @Autowired
    private ThreadLocalGuardScopeManager scopeManager;

    @BeforeEach
    void setUp() {
        purchaseOrderRepository.deleteAll();
        memberRepository.deleteAll();

        for (int memberIndex = 1; memberIndex <= 3; memberIndex++) {
            Member member = new Member("member-" + memberIndex);
            member.addOrder(new PurchaseOrder("item-A-" + memberIndex));
            member.addOrder(new PurchaseOrder("item-B-" + memberIndex));
            memberRepository.save(member);
        }
    }

    @Test
    void nPlusOneQueryShouldExceedBudget() {
        GuardPolicy strictPolicy = new GuardPolicy(0, 3, 1);

        // findAll + 각 member의 lazy orders 조회가 반복되어 예산을 넘긴다.
        assertThatThrownBy(() -> QueryGuardAssertions.assertWithin(
                scopeManager,
                strictPolicy,
                memberQueryService::countOrdersWithNPlusOne
        ))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("SELECT 쿼리 수가 제한을 초과했습니다");
    }

    @Test
    void fetchJoinQueryShouldPassBudget() {
        GuardPolicy fetchJoinPolicy = new GuardPolicy(0, 1, 1);

        // fetch join은 member + orders를 한 번에 가져와 N+1을 피한다.
        int orderCount = QueryGuardAssertions.assertWithin(
                scopeManager,
                fetchJoinPolicy,
                memberQueryService::countOrdersWithFetchJoin
        );

        assertThat(orderCount).isEqualTo(6);
    }
}
