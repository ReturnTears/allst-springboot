package com.allst.boot.subscription.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeskStats {
    private int bookCount;
    private int authorCount;
    private int readerCount;
    private int categoryCount;
    private int activeMembershipCount;
}
