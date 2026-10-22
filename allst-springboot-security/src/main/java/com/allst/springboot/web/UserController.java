package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.rbac.UserAdminService;
import com.allst.springboot.web.dto.EnabledRequest;
import com.allst.springboot.web.dto.PasswordResetRequest;
import com.allst.springboot.web.dto.UserCreateRequest;
import com.allst.springboot.web.dto.UserView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户管理。读需要 user:read，写需要 user:write。见 docs/02-authorization.md。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAdminService users;

    public UserController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping
    public ApiResult<List<UserView>> list() {
        return ApiResult.ok(users.list());
    }

    @PostMapping
    public ApiResult<UserView> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResult.ok(users.create(request));
    }

    @PutMapping("/{id}/password")
    public ApiResult<String> reset(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        users.resetPassword(id, request.getNewPassword());
        return ApiResult.ok("密码已重置");
    }

    @PutMapping("/{id}/enabled")
    public ApiResult<UserView> enabled(@PathVariable Long id, @Valid @RequestBody EnabledRequest request) {
        return ApiResult.ok(users.setEnabled(id, request.isEnabled()));
    }
}
