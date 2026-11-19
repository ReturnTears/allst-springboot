package com.allst.springboot.ddl.meta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 表元数据仓储。生成器从库里读取元数据时只认这个接口，
 * 离线生成（请求体直接带元数据）则完全不碰它。
 */
public interface TableMetaRepository extends JpaRepository<TableMeta, Long> {

    List<TableMeta> findByDialectOrderByIdAsc(String dialect);

    Optional<TableMeta> findBySchemaNameAndTableName(String schemaName, String tableName);
}
