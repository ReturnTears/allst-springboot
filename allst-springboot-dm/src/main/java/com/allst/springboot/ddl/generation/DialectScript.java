package com.allst.springboot.ddl.generation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 单一方言生成的一份脚本（可对应一个导出文件）。 */
@Getter
public final class DialectScript {

    private final String dialect;
    /** 该方言是否走了 GENERIC 兜底 */
    private final boolean fallback;
    private final List<TableDdl> tables;
    /** 含文件头的完整脚本文本 */
    private final String sql;

    public DialectScript(String dialect, boolean fallback, List<TableDdl> tables, String header) {
        this.dialect = dialect;
        this.fallback = fallback;
        this.tables = Collections.unmodifiableList(new ArrayList<>(tables));
        String body = tables.stream().map(TableDdl::getSql).collect(Collectors.joining("\n\n"));
        this.sql = header + "\n\n" + body;
    }
}
