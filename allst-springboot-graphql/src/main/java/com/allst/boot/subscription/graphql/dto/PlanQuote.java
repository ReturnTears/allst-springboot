package com.allst.boot.subscription.graphql.dto;

import com.allst.boot.subscription.model.MembershipPlan;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanQuote {
    private MembershipPlan plan;
    private int days;
    private double price;
}
