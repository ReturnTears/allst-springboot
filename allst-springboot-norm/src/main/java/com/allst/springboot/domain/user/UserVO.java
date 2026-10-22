package com.allst.springboot.domain.user;

import com.allst.springboot.crypto.Desensitize;
import com.allst.springboot.crypto.DesensitizeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对外用户视图。敏感字段只在序列化时脱敏，服务内部仍可使用明文。
 */
@Data
@Schema(description = "用户信息，敏感字段已脱敏")
public class UserVO {

    private Long id;
    private String username;

    @Desensitize(DesensitizeType.NAME)
    private String realName;

    @Desensitize(DesensitizeType.PHONE)
    private String phone;

    @Desensitize(DesensitizeType.EMAIL)
    private String email;

    @Desensitize(DesensitizeType.ID_CARD)
    private String idCard;

    @Schema(description = "1 启用，0 停用")
    private Integer status;
    private String createdBy;
    private LocalDateTime createdAt;
}
