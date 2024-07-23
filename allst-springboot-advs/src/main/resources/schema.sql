-- CREATE TABLE employees (
--    id INT AUTO_INCREMENT PRIMARY KEY,
--    first_name VARCHAR(50) NOT NULL,
--    last_name VARCHAR(50) NOT NULL,
--    email VARCHAR(100) UNIQUE,
--    hire_date DATE NOT NULL,
--    job_title VARCHAR(50),
--    salary DECIMAL(10, 2),
--    department_id INT,
--    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--
-- -- 外键约束
--    FOREIGN KEY (department_id) REFERENCES departments(id),
--
-- -- 检查约束（注意：MySQL 仅在 InnoDB 存储引擎中支持 CHECK 约束）
--    CHECK (salary >= 0)
-- ) ENGINE = InnoDB;

drop table if exists employee;	-- 表名若存在则删除
create table employee (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          last_name VARCHAR(50) NOT NULL,
                          email VARCHAR(100) UNIQUE,
                          gender TINYINT NOT NULL,
                          dId INT NOT NULL
) ENGINE=INNODB ;

drop table if exists department;	-- 表名若存在则删除
create table department (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            department_name VARCHAR(50) NOT NULL,
                            employee_nums INT NOT NULL
) ENGINE=INNODB ;
insert into department (department_name, employee_nums) values ('市场部', 1000), ('研发部', 200);

drop table if exists t_user;	-- 表名若存在则删除
create table t_user (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(50) NOT NULL,
                        email VARCHAR(50) NOT NULL,
                        age INT NOT NULL
) ENGINE=INNODB ;
insert into t_user (name, email, age) VALUES ('张三', 'zhangsan@qq.com', 30),('李四', 'lisi@qq.com', 32);

SET GLOBAL wait_timeout = 28800;  -- 非交互式连接
SET GLOBAL interactive_timeout = 28800;  -- 交互式连接
