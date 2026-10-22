package com.allst.springboot.controller;

import com.allst.springboot.annotation.Idempotent;
import com.allst.springboot.annotation.RequireSign;
import com.allst.springboot.common.api.Result;
import com.allst.springboot.common.header.Operators;
import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.common.header.RequestMeta;
import com.allst.springboot.domain.user.IdsCommand;
import com.allst.springboot.domain.user.UserBatchCreateCommand;
import com.allst.springboot.domain.user.UserCommandService;
import com.allst.springboot.domain.user.UserCreateCommand;
import com.allst.springboot.domain.user.UserStatusCommand;
import com.allst.springboot.domain.user.UserUpdateCommand;
import com.allst.springboot.domain.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户写入。POST 创建，PUT 全量改资料，PATCH 只改状态，DELETE 删除。
 * 创建类接口要求幂等键。对应规范：9、15、17、18。
 */
@Validated
@RestController
@RequireSign
@RequestMapping("/api/v1/users")
@Tag(name = "用户写入")
public class UserCommandController {

    private final UserCommandService userCommandService;

    public UserCommandController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping
    @Idempotent
    @Operation(summary = "创建用户", description = "需要 X-Idempotent-Key。同一键的成功请求会返回第一次的结果。")
    public ResponseEntity<Result<UserVO>> create(@Valid @RequestBody UserCreateCommand command) {
        UserVO vo = userCommandService.create(command, operator());
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok(vo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "全量修改资料", description = "不修改用户名和状态。")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateCommand command) {
        return Result.ok(userCommandService.update(id, command));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改启用状态")
    public Result<UserVO> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusCommand command) {
        return Result.ok(userCommandService.changeStatus(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userCommandService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/batch")
    @Idempotent
    @Operation(summary = "批量创建用户", description = "整批一个事务。任一条不合法或用户名冲突，整批不落库。")
    public ResponseEntity<Result<List<Long>>> batchCreate(@Valid @RequestBody UserBatchCreateCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok(userCommandService.batchCreate(command, operator())));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除用户", description = "任一编号不存在则整批取消。请求体为 JSON，调用方需确认客户端不会丢掉 DELETE body。")
    public Result<Void> batchDelete(@Valid @RequestBody IdsCommand command) {
        userCommandService.batchDelete(command.getIds());
        return Result.ok(null);
    }

    private String operator() {
        RequestMeta meta = RequestContext.get();
        return Operators.normalize(meta == null ? null : meta.getOperator());
    }
}
