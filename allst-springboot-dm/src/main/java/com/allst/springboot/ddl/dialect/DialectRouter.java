package com.allst.springboot.ddl.dialect;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 方言路由。Spring 把所有 DdlDialect bean 注入进来建索引，
 * 新增方言只要加一个 @Component，路由代码零改动——这是「策略 + 工厂」在 Spring 里的标准落地。
 *
 * <p>未注册的方言落到 GENERIC 兜底（ANSI SQL + 警告），保证「没见过的库」也能导出可用 DDL，
 * 而不是启动失败。isFallback() 让上层提示用户「该方言是通用降级输出」。
 */
@Component
public class DialectRouter {

    private final Map<String, DdlDialect> dialects = new LinkedHashMap<>();
    private final DdlDialect fallback;

    public DialectRouter(List<DdlDialect> beans) {
        for (DdlDialect bean : beans) {
            dialects.put(normalize(bean.name()), bean);
        }
        DdlDialect generic = dialects.get("GENERIC");
        if (generic == null) {
            throw new IllegalStateException("缺少 GENERIC 兜底方言实现");
        }
        this.fallback = generic;
    }

    public DdlDialect route(String name) {
        DdlDialect hit = dialects.get(normalize(name));
        return hit != null ? hit : fallback;
    }

    public boolean isFallback(String name) {
        return !dialects.containsKey(normalize(name));
    }

    public List<DdlDialect> all() {
        return new ArrayList<>(dialects.values());
    }

    public List<String> names() {
        return new ArrayList<>(dialects.keySet());
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
