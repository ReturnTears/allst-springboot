package com.allst.springboot.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 全量修改资料。不包含状态和用户名，那两项由各自的接口负责。
 */
@Data
@Schema(description = "全量修改用户资料")
public class UserUpdateCommand {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度超出限制")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度超出限制")
    private String email;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;
}
