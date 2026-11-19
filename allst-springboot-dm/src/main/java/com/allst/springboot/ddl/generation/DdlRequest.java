package com.allst.springboot.ddl.generation;

import com.allst.springboot.ddl.meta.TableMeta;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成请求。tables 非空时走「离线生成」（不读库）；为空时由 Controller 从库里取该方言的表。
 * dialect 非空时覆盖元数据自带的方言，方便「同一批表看看在 Impala 下长什么样」。
 */
@Getter
@Setter
public class DdlRequest {

    private String dialect;
    private List<TableMeta> tables = new ArrayList<>();
}
