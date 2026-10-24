package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.graphql.dto.DeskStats;
import com.allst.boot.subscription.service.DeskService;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.stereotype.Component;

@Component
public class DeskOperations implements GraphQLQueryResolver {
    private final DeskService deskService;

    public DeskOperations(DeskService deskService) {
        this.deskService = deskService;
    }

    public DeskStats deskStats() {
        return deskService.stats();
    }
}
