package com.allst.boot.subscription.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageInfo {
    private int page;
    private int size;
    private int total;
    private int totalPages;
}
