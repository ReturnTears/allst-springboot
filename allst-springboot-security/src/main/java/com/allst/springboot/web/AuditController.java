package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.domain.repo.LoginAuditRepository;
import com.allst.springboot.web.dto.LoginAuditView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录审计。链上要求 audit:read，方法上再写一次。alice 没有这个权限，请求在过滤链就被 403，进不了这个方法。
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final LoginAuditRepository audits;

    public AuditController(LoginAuditRepository audits) {
        this.audits = audits;
    }

    @GetMapping("/logins")
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResult<List<LoginAuditView>> logins() {
        List<LoginAuditView> views = audits.findTop50ByOrderByCreatedAtDesc().stream()
                .map(LoginAuditView::from)
                .collect(Collectors.toList());
        return ApiResult.ok(views);
    }
}
