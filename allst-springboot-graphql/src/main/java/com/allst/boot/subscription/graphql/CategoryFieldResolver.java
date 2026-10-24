package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.model.Category;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class CategoryFieldResolver implements GraphQLResolver<Category> {
    public CompletableFuture<Integer> bookCount(Category category, DataFetchingEnvironment environment) {
        DataLoader<Integer, Integer> loader = environment.getDataLoader(LoaderNames.BOOK_COUNT_BY_CATEGORY_ID);
        return loader.load(category.getId()).thenApply(count -> count == null ? 0 : count);
    }
}
