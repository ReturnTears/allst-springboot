drop table if exists sys_def;
create table sys_def
(
    def_id int(11) not null comment 'ID',
    dept_id int(11) not null comment '部门ID',
    primary key (def_id, dept_id)
) engine = innodb
  default charset = utf8 comment = '关联表';