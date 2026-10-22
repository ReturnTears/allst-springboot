package com.allst.springboot.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PasswordChangeRequest {

    @NotBlank
    @Size(max = 72)
    private String oldPassword;

    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
