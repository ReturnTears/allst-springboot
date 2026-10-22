package com.allst.springboot.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class ExpiredPasswordRequest {

    @NotBlank
    @Size(max = 64)
    private String username;

    @NotBlank
    @Size(max = 72)
    private String oldPassword;

    @NotBlank
    @Size(max = 72)
    private String newPassword;
}
