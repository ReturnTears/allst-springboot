package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.model.Membership;
import com.allst.boot.subscription.model.Reader;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class ReaderFieldResolver implements GraphQLResolver<Reader> {
    public CompletableFuture<List<Membership>> memberships(Reader reader, DataFetchingEnvironment environment) {
        DataLoader<Integer, List<Membership>> loader = environment.getDataLoader(LoaderNames.MEMBERSHIPS_BY_READER_ID);
        return loader.load(reader.getId()).thenApply(items -> items == null ? List.of() : items);
    }
}
