package com.allst.springboot.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SessionView {

    private final String sessionIdPrefix;
    private final String username;
    private final LocalDateTime lastRequest;
}
