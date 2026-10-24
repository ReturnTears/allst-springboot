package com.allst.boot.subscription.graphql.input;

import com.allst.boot.subscription.model.MembershipPlan;
import com.allst.boot.subscription.model.MembershipStatus;
import lombok.Data;

@Data
public class MembershipFilter {
    private Integer readerId;
    private Integer bookId;
    private MembershipStatus status;
    private MembershipPlan plan;
    private String keyword;
}
