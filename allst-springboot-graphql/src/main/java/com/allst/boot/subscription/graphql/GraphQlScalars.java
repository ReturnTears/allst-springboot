package com.allst.boot.subscription.graphql;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class GraphQlScalars {
    public static final GraphQLScalarType DATE = GraphQLScalarType.newScalar()
            .name("Date")
            .description("ISO-8601 日期，例如 2026-10-24")
            .coercing(new Coercing<LocalDate, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof LocalDate) {
                        return dataFetcherResult.toString();
                    }
                    if (dataFetcherResult instanceof java.sql.Date) {
                        return ((java.sql.Date) dataFetcherResult).toLocalDate().toString();
                    }
                    throw new CoercingSerializeException("期望 LocalDate，实际是 " + dataFetcherResult);
                }

                @Override
                public LocalDate parseValue(Object input) {
                    return parse(String.valueOf(input));
                }

                @Override
                public LocalDate parseLiteral(Object input) {
                    if (input instanceof StringValue) {
                        return parse(((StringValue) input).getValue());
                    }
                    throw new CoercingParseLiteralException("日期需要写成字符串");
                }

                private LocalDate parse(String text) {
                    try {
                        return LocalDate.parse(text);
                    } catch (DateTimeParseException ex) {
                        throw new CoercingParseValueException("日期格式应为 yyyy-MM-dd");
                    }
                }
            })
            .build();

    public static final GraphQLScalarType DATE_TIME = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("ISO-8601 本地日期时间，例如 2026-10-24T09:30:00")
            .coercing(new Coercing<LocalDateTime, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof LocalDateTime) {
                        return dataFetcherResult.toString();
                    }
                    if (dataFetcherResult instanceof java.sql.Timestamp) {
                        return ((java.sql.Timestamp) dataFetcherResult).toLocalDateTime().toString();
                    }
                    throw new CoercingSerializeException("期望 LocalDateTime，实际是 " + dataFetcherResult);
                }

                @Override
                public LocalDateTime parseValue(Object input) {
                    return parse(String.valueOf(input));
                }

                @Override
                public LocalDateTime parseLiteral(Object input) {
                    if (input instanceof StringValue) {
                        return parse(((StringValue) input).getValue());
                    }
                    throw new CoercingParseLiteralException("日期时间需要写成字符串");
                }

                private LocalDateTime parse(String text) {
                    String normalized = text.trim().replace(' ', 'T');
                    try {
                        return LocalDateTime.parse(normalized);
                    } catch (DateTimeParseException ex) {
                        throw new CoercingParseValueException("日期时间格式应为 yyyy-MM-ddTHH:mm:ss");
                    }
                }
            })
            .build();

    private GraphQlScalars() {
    }
}
