CREATE DATABASE IF NOT EXISTS demo DEFAULT CHARACTER SET utf8mb4;
USE demo;

-- Canal 只需要复制权限，不要用业务账号去 dump binlog。
CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED WITH mysql_native_password BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;

-- 同步示例表。主键是下游幂等写入的文档 ID，没有主键的表会被客户端跳过。
CREATE TABLE IF NOT EXISTS t_product (
    id         BIGINT         NOT NULL PRIMARY KEY,
    name       VARCHAR(128)   NOT NULL,
    price      DECIMAL(10, 2) NOT NULL,
    stock      INT            NOT NULL,
    updated_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 不要在这里插入业务数据。Canal 首次启动从当前位点向后读，初始化脚本里的 INSERT 发生在订阅之前，不会进入 MQ。
