package com.allst.boot.subscription.graphql;

import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScalarConfiguration {
    @Bean
    public GraphQLScalarType dateScalar() {
        return GraphQlScalars.DATE;
    }

    @Bean
    public GraphQLScalarType dateTimeScalar() {
        return GraphQlScalars.DATE_TIME;
    }
}
