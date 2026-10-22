package com.allst.springboot.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "只能包含字母、数字和下划线")
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Size(max = 64)
    private String displayName;

    private List<String> roleCodes = new ArrayList<>();
}
