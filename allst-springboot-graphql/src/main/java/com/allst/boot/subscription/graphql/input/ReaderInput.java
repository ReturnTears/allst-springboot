package com.allst.boot.subscription.graphql.input;

import lombok.Data;

@Data
public class ReaderInput {
    private String username;
    private String email;
    private String phone;
}
