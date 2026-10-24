package com.allst.boot.subscription.graphql.input;

import com.allst.boot.subscription.model.MembershipPlan;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MembershipInput {
    private Integer readerId;
    private Integer bookId;
    private MembershipPlan plan;
    private LocalDate startDate;
    private String remark;
}
