package com.allst.springboot.web;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.security.extension.SecurityCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 当前装载的协议和扩展点。见 docs/08-extensible-architecture.md。
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
