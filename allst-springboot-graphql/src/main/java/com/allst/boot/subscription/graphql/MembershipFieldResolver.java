package com.allst.boot.subscription.graphql;

import com.allst.boot.model.Book;
import com.allst.boot.subscription.model.Membership;
import com.allst.boot.subscription.model.Reader;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class MembershipFieldResolver implements GraphQLResolver<Membership> {
    public CompletableFuture<Reader> reader(Membership membership, DataFetchingEnvironment environment) {
        DataLoader<Integer, Reader> loader = environment.getDataLoader(LoaderNames.READER_BY_ID);
        return loader.load(membership.getReaderId());
    }

    public CompletableFuture<Book> book(Membership membership, DataFetchingEnvironment environment) {
        DataLoader<Integer, Book> loader = environment.getDataLoader(LoaderNames.BOOK_BY_ID);
        return loader.load(membership.getBookId());
    }
}
