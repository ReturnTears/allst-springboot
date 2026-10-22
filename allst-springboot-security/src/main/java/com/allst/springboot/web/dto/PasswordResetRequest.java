package com.allst.springboot.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PasswordResetRequest {

    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
