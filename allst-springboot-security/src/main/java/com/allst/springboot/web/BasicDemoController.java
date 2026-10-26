package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.web.dto.UserView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP Basic 示例。没有控制器里的认证代码：BasicAuthenticationFilter 已经把用户放进上下文。
 * 调用时请求头是 {@code Authorization: Basic base64(用户名:口令)}，不是 Bearer。
 */
@RestController
@RequestMapping("/basic")
public class BasicDemoController {

    @GetMapping("/me")
    public ApiResult<UserView> me(Authentication authentication) {
        return ApiResult.ok(UserView.from((SecurityUser) authentication.getPrincipal()));
    }
}
