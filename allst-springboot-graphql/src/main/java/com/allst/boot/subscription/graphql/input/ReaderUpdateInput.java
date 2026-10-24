package com.allst.boot.subscription.graphql.input;

import com.allst.boot.subscription.model.ReaderStatus;
import lombok.Data;

@Data
public class ReaderUpdateInput {
    private Integer id;
    private String email;
    private String phone;
    private ReaderStatus status;
}
