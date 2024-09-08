# Mybatis-Plus

## sql脚本
```sql
use mybatis_plus;
show tables;
create table `book` (
    id int primary key auto_increment,
    name varchar(20) not null ,
    author varchar(20) not null,
    status int default 1 comment '1-上架，2-在管，3-借出，4-损坏，5-丢失，6-下架',
    create_time datetime default current_timestamp,
    update_time datetime default current_timestamp on update current_timestamp
) engine=innodb default charset=utf8mb4 comment='图书表';
select * from book;
```

