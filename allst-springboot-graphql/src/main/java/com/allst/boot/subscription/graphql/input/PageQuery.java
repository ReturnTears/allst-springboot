package com.allst.boot.subscription.graphql.input;

import lombok.Data;

@Data
public class PageQuery {
    private Integer page;
    private Integer size;
    private String sortBy;
    private SortDirection direction;
}
