package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.graphql.dto.MembershipPage;
import com.allst.boot.subscription.graphql.dto.PlanQuote;
import com.allst.boot.subscription.graphql.input.MembershipFilter;
import com.allst.boot.subscription.graphql.input.MembershipInput;
import com.allst.boot.subscription.graphql.input.MembershipUpdateInput;
import com.allst.boot.subscription.graphql.input.PageQuery;
import com.allst.boot.subscription.model.Membership;
import com.allst.boot.subscription.service.MembershipService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MembershipOperations implements GraphQLQueryResolver, GraphQLMutationResolver {
    private final MembershipService membershipService;

    public MembershipOperations(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public Membership membership(Integer id) {
        return membershipService.get(id);
    }

    public MembershipPage memberships(MembershipFilter filter, PageQuery page) {
        return membershipService.search(filter, page);
    }

    public List<Membership> readerMemberships(Integer readerId) {
        return membershipService.listByReader(readerId);
    }

    public List<PlanQuote> planQuotes() {
        return membershipService.quotes();
    }

    public Membership createMembership(MembershipInput input) {
        return membershipService.create(input);
    }

    public Membership updateMembership(MembershipUpdateInput input) {
        return membershipService.update(input);
    }

    public Membership cancelMembership(Integer id) {
        return membershipService.cancel(id);
    }

    public boolean deleteMembership(Integer id) {
        return membershipService.delete(id);
    }
}
