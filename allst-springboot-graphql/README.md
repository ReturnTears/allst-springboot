# Graphql
```text
环境介绍
Java11 + SpringBoot2.7.4 + Graphql 14.0.0 + kotlin1.9.22 

Graphql Tool 地址：
http://127.0.0.1:8088/graphiql
```

## Query
```text
-- Query 1 查询一个列表
query{
  findAll{
    id,
    name,
    pageCount
  }
}

-- Query 2 查询多个列表
query{
  findAll{
    id,
    name,
    pageCount
  }
  getAuthorByBookId(bookId:5){
    id,
    firstName,
    lastName
  }
}

-- Query 3 嵌套查询
query{
  findAll{
    id,
    name,
    pageCount,
    author{
      id,
      firstName,
      lastName
    }
  }
}

-- Query 4 按照指定条件查询
query{
  getAuthorById(id:1){
    id,
    firstName,
    lastName,
    bookId
  }
}
```

## Mutation
```text
-- Mutation 新增
mutation newBookMethod($name: String!, $pageCount: String) {
  newBook(name: $name, pageCount: $pageCount) {
    name
    pageCount
  }
}
-- 变量
variables: 
{
  "name": "Java",
  "pageCount": "845"
}

-- Mutation 修改
mutation updateAuthorMethod($id: Int!, $firstName: String) {
  updateAuthor(id: $id, firstName: $firstName) {
    id
    firstName
  }
}
-- 变量
{
  "id": 7,
  "firstName": "Sun"
}

-- Mutation 修改2
mutation updateAuthorMethod($id: Int!, $firstName: String, $lastName: String) {
  updateAuthorById(id: $id, firstName: $firstName, lastName: $lastName) {
    id
    firstName
    lastName
  }
}
-- 变量
{
  "id": 7,
  "firstName": "Sun",
  "lastName": "wu kong"
}
```