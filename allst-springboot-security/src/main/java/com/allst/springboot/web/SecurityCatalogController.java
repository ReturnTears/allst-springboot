package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.extension.SecurityCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 运行时目录。{@code GET /api/security/modules} 需要登录。
 * 它不做授权判断，只是把容器里已经装好的协议、登录守卫和令牌扩展列出来，方便对照源码。
 */
@RestController
@RequestMapping("/api/security")
public class SecurityCatalogController {

    private final SecurityCatalog catalog;

    public SecurityCatalogController(SecurityCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/modules")
    public ApiResult<Map<String, Object>> modules() {
        return ApiResult.ok(catalog.describe());
    }
}
