# Graphql
```text
Java11 + SpringBoot2.7.4 + Graphql 12.0.0 

```

## Query
```text
query{
  allBook{
    id,
    name,
    pageCount
  }
}
```

## Mutation
```text
mutation newBookMrthod($name: String!, $pageCount: String) {
  newBook(name: $name, pageCount: $pageCount) {
    name
    pageCount
  }
}
variables: 
{
  "name": "Java",
  "pageCount": "845"
}
```