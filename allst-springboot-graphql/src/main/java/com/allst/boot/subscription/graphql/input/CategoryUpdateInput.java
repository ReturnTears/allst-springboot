package com.allst.boot.subscription.graphql.input;

import lombok.Data;

@Data
public class CategoryUpdateInput {
    private Integer id;
    private String name;
    private String description;
}
