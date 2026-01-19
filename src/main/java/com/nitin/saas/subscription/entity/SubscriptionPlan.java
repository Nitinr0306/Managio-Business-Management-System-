package com.nitin.saas.subscription.entity;

import com.nitin.saas.business.entity.Business;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "subscription_plans",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"business_id", "code"})
        }
)
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int durationInDays;

    protected SubscriptionPlan() {}

    public SubscriptionPlan(
            Business business,
            String name,
            String code,
            int price,
            int durationInDays
    ) {
        this.business = business;
        this.name = name;
        this.code = code;
        this.price = price;
        this.durationInDays = durationInDays;
    }

    public int getDurationInDays() {
        return durationInDays;
    }
}
