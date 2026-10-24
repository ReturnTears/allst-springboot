package com.allst.boot.subscription.graphql;

import com.allst.boot.model.Book;
import com.allst.boot.subscription.model.Category;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class BookCategoryFieldResolver implements GraphQLResolver<Book> {
    public CompletableFuture<Category> category(Book book, DataFetchingEnvironment environment) {
        DataLoader<Integer, Category> loader = environment.getDataLoader(LoaderNames.CATEGORY_BY_BOOK_ID);
        return loader.load(book.getId());
    }
}
