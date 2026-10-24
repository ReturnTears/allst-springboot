package com.allst.boot.subscription.graphql.dto;

import com.allst.boot.subscription.model.Membership;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MembershipPage {
    private List<Membership> items;
    private PageInfo pageInfo;
}
