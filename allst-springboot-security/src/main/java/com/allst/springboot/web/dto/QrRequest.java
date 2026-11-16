package com.allst.springboot.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/** 手机扫码 / 确认 / 取消时提交的二维码编号。 */
@Getter
@Setter
public class QrRequest {

    @NotBlank
    private String qrId;
}