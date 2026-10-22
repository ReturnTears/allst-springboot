package com.allst.springboot.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordPolicyView {

    private final int minLength;
    private final int maxLength;
    private final int historySize;
    private final int maxAgeDays;
    private final String rule;
}
