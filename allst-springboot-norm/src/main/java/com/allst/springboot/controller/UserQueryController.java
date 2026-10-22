package com.allst.springboot.controller;

import com.allst.springboot.annotation.RequireSign;
import com.allst.springboot.common.api.PageData;
import com.allst.springboot.common.api.Result;
import com.allst.springboot.domain.user.UserPageQuery;
import com.allst.springboot.domain.user.UserQueryService;
import com.allst.springboot.domain.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户查询。写入在 {@link UserCommandController}，避免查询条件和写模型缠在一起。
 * 对应规范：15 请求方式，18 职责单一。
 */
@Validated
@RestController
@RequireSign
@RequestMapping("/api/v1/users")
@Tag(name = "用户查询")
public class UserQueryController {

    private final UserQueryService userQueryService;

    public UserQueryController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping
    @Operation(summary = "分页查询用户", description = "页码从 0 开始。每页条数超过配置上限会被拒绝，而不是静默截断。")
    public Result<PageData<UserVO>> page(@Valid UserPageQuery query) {
        return Result.ok(userQueryService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "按编号查询用户")
    public Result<UserVO> get(@PathVariable Long id) {
        return Result.ok(userQueryService.get(id));
    }
}
