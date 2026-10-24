package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.model.Membership;
import graphql.kickstart.tools.GraphQLSubscriptionResolver;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

@Component
public class MembershipSubscriptionResolver implements GraphQLSubscriptionResolver {
    private final MembershipEventBus eventBus;

    public MembershipSubscriptionResolver(MembershipEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public Publisher<Membership> membershipChanged() {
        return eventBus.flux();
    }
}
