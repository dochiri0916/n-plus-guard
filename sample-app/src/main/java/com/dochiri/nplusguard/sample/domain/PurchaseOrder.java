package com.dochiri.nplusguard.sample.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(String itemName) {
        this.itemName = itemName;
    }

    public Long getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public Member getMember() {
        return member;
    }

    void assignMember(Member member) {
        this.member = member;
    }
}
