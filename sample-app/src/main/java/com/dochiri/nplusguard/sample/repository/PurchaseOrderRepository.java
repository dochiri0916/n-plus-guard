package com.dochiri.nplusguard.sample.repository;

import com.dochiri.nplusguard.sample.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
}
