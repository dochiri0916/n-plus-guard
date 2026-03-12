package com.dochiri.nplusguard.sample.repository;

import com.dochiri.nplusguard.sample.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("select distinct m from Member m left join fetch m.orders")
    List<Member> findAllWithOrders();
}
