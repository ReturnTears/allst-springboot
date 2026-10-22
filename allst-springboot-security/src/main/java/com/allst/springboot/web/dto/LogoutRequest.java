package com.allst.springboot.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutRequest {

    private String refreshToken;
}
