package com.dochiri.nplusguard.sample;

import com.dochiri.nplusguard.core.policy.GuardPolicy;
import com.dochiri.nplusguard.core.scope.ThreadLocalGuardScopeManager;
import com.dochiri.nplusguard.sample.domain.Member;
import com.dochiri.nplusguard.sample.domain.PurchaseOrder;
import com.dochiri.nplusguard.sample.repository.MemberRepository;
import com.dochiri.nplusguard.sample.repository.PurchaseOrderRepository;
import com.dochiri.nplusguard.sample.service.MemberQueryService;
import com.dochiri.nplusguard.testsupport.EnableQueryGuard;
import com.dochiri.nplusguard.testsupport.QueryBudget;
import com.dochiri.nplusguard.testsupport.QueryGuardAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@EnableQueryGuard
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
    @DisplayName("주문 컬렉션을 지연 로딩으로 조회하면 쿼리 예산을 초과한다")
    void nPlusOneQueryShouldExceedBudget() {
        // given
        GuardPolicy strictPolicy = new GuardPolicy(0, 3, 1);

        // when
        Throwable thrown = catchThrowable(() -> QueryGuardAssertions.assertWithin(
                scopeManager,
                strictPolicy,
                memberQueryService::countOrdersWithNPlusOne
        ));

        // then
        assertThat(thrown)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("SELECT 쿼리 수가 제한을 초과했습니다");
    }

    @Test
    @DisplayName("fetch join으로 조회하면 쿼리 예산 안에서 통과한다")
    void fetchJoinQueryShouldPassBudget() {
        // given
        GuardPolicy fetchJoinPolicy = new GuardPolicy(0, 1, 1);

        // when
        int orderCount = QueryGuardAssertions.assertWithin(
                scopeManager,
                fetchJoinPolicy,
                memberQueryService::countOrdersWithFetchJoin
        );

        // then
        assertThat(orderCount).isEqualTo(6);
    }

    @Test
    @DisplayName("쿼리 예산 어노테이션을 사용해도 fetch join 조회는 통과한다")
    @QueryBudget(maxSelectQueries = 1, maxRepeatedSelectExecutions = 1)
    void fetchJoinQueryShouldPassBudgetWithExtension() {
        // given

        // when
        int orderCount = memberQueryService.countOrdersWithFetchJoin();

        // then
        assertThat(orderCount).isEqualTo(6);
    }
}
