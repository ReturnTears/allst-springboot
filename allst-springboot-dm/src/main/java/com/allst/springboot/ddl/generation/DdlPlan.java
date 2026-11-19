package com.allst.springboot.ddl.generation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一批元数据的生成计划：按方言分组，每组一个脚本。 */
@Getter
public final class DdlPlan {

    private final List<DialectScript> scripts;
    private final int tableCount;

    public DdlPlan(List<DialectScript> scripts) {
        this.scripts = Collections.unmodifiableList(new ArrayList<>(scripts));
        this.tableCount = scripts.stream().mapToInt(s -> s.getTables().size()).sum();
    }
}
