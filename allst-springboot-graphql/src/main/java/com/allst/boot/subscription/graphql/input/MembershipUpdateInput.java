package com.allst.boot.subscription.graphql.input;

import com.allst.boot.subscription.model.MembershipPlan;
import com.allst.boot.subscription.model.MembershipStatus;
import lombok.Data;

@Data
public class MembershipUpdateInput {
    private Integer id;
    private MembershipPlan plan;
    private MembershipStatus status;
    private String remark;
}
