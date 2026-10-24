package com.allst.boot.subscription.graphql.input;

import lombok.Data;

@Data
public class BookUpdateInput {
    private Integer id;
    private String name;
    private String pageCount;
}
