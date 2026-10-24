package com.allst.boot.subscription.graphql.input;

import com.allst.boot.subscription.model.ReaderStatus;
import lombok.Data;

@Data
public class ReaderFilter {
    private String keyword;
    private ReaderStatus status;
}
