package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 方言策略接口。每一种数据库一个实现，Spring 自动注入后由 DialectRouter 建索引。
 *
 * <p>新增方言 = 新增一个 @Component 实现 + 声明能力与类型映射，路由与模板零改动（开闭原则）。
 */
public interface DdlDialect {

    /** 方言标识，与 TableMeta.dialect 对应，如 MYSQL / IMPALA */
    String name();

    /** 该方言支持的 DDL 能力集合 */
    Set<Capability> capabilities();

    default boolean supports(Capability capability) {
        return capabilities().contains(capability);
    }

    /** 是否认识这个逻辑类型；不认识就明确报错，绝不猜物理类型 */
    boolean supportsType(String logicalType);

    /** 该方言注册的逻辑类型清单，供能力矩阵接口展示 */
    default List<String> supportedTypes() {
        return Collections.emptyList();
    }

    /** 逻辑类型 → 物理类型（含长度/精度），由各方言自定义 */
    String renderType(ColumnMeta column);

    /** 标识符引号策略（反引号 / 双引号 / 方括号 / 无） */
    String quote(String identifier);

    /** 生成主 DDL；实现由模板基类以 final 固定流程 */
    DdlOutput generate(TableMeta table);

    default String autoIncrementClause(ColumnMeta column) {
        return "";
    }

    default String inlineColumnComment(ColumnMeta column) {
        return "";
    }

    default String primaryKeyClause(TableMeta table) {
        return "";
    }

    default String inlineTableComment(TableMeta table) {
        return "";
    }

    default String partitionClause(TableMeta table) {
        return "";
    }

    default String tableOptionsClause(TableMeta table) {
        return "";
    }

    /** 主 DDL 之后的附加语句，如 PostgreSQL/Oracle 的 COMMENT ON */
    default List<String> postDdlStatements(TableMeta table) {
        return Collections.emptyList();
    }
}
