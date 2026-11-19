package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.TableMeta;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次生成的产物：主 DDL 文本 + 警告。
 * 警告用于「方言不支持某能力」这类降级提示，不阻断生成。
 */
@Getter
public final class DdlOutput {

    private final String sql;
    private final List<String> warnings;

    public DdlOutput(String sql, List<String> warnings) {
        this.sql = sql;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }
}
