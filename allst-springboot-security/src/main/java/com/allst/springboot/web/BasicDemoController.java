package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.user.SecurityUser;
import com.allst.springboot.web.dto.UserView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP Basic 示例。见 docs/06-protocol-integration.md。
 */
@RestController
@RequestMapping("/basic")
public class BasicDemoController {

    @GetMapping("/me")
    public ApiResult<UserView> me(Authentication authentication) {
        return ApiResult.ok(UserView.from((SecurityUser) authentication.getPrincipal()));
    }
}
