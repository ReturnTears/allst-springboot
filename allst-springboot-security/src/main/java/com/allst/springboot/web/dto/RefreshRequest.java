package com.allst.springboot.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class RefreshRequest {

    @NotBlank
    private String refreshToken;
}
