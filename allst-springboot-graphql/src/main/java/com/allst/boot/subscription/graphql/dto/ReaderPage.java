package com.allst.boot.subscription.graphql.dto;

import com.allst.boot.subscription.model.Reader;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReaderPage {
    private List<Reader> items;
    private PageInfo pageInfo;
}
