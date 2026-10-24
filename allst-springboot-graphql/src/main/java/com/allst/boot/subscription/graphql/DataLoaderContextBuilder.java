package com.allst.boot.subscription.graphql;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

@Component
public class DataLoaderContextBuilder implements GraphQLServletContextBuilder {
    private final LoaderRegistryFactory loaderRegistryFactory;

    public DataLoaderContextBuilder(LoaderRegistryFactory loaderRegistryFactory) {
        this.loaderRegistryFactory = loaderRegistryFactory;
    }

    @Override
    public GraphQLKickstartContext build(HttpServletRequest request, HttpServletResponse response) {
        return GraphQLKickstartContext.of(loaderRegistryFactory.create());
    }

    @Override
    public GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest) {
        return GraphQLKickstartContext.of(loaderRegistryFactory.create());
    }

    @Override
    public GraphQLKickstartContext build() {
        return GraphQLKickstartContext.of(loaderRegistryFactory.create());
    }
}
