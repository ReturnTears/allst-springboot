create table book (
    id BIGINT not null primary key ,
    name varchar(50) not null ,
    pageCount varchar(10) not null
);
insert into book (id, name, pageCount) values (9, 'JavaScript', '1000'), (10, 'C#', '2000'), (11, 'C', '3000'), (12, 'Python 3.12', '4000'), (13, 'Golang', '5000');
select * from book;
