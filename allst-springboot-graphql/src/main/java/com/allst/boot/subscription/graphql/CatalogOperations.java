package com.allst.boot.subscription.graphql;

import com.allst.boot.model.Book;
import com.allst.boot.subscription.graphql.input.BookUpdateInput;
import com.allst.boot.subscription.service.CatalogService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogOperations implements GraphQLQueryResolver, GraphQLMutationResolver {
    private final CatalogService catalogService;

    public CatalogOperations(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public Book bookById(Integer id) {
        return catalogService.get(id);
    }

    public List<Book> searchBooks(String keyword) {
        return catalogService.search(keyword);
    }

    public Book updateBook(BookUpdateInput input) {
        return catalogService.update(input);
    }

    public Book assignBookCategory(Integer bookId, Integer categoryId) {
        return catalogService.assignCategory(bookId, categoryId);
    }

    public boolean clearBookCategory(Integer bookId) {
        return catalogService.clearCategory(bookId);
    }
}
