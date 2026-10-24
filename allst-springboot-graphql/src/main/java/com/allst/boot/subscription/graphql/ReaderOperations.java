package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.graphql.dto.ReaderPage;
import com.allst.boot.subscription.graphql.input.PageQuery;
import com.allst.boot.subscription.graphql.input.ReaderFilter;
import com.allst.boot.subscription.graphql.input.ReaderInput;
import com.allst.boot.subscription.graphql.input.ReaderUpdateInput;
import com.allst.boot.subscription.model.Reader;
import com.allst.boot.subscription.service.ReaderService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.stereotype.Component;

@Component
public class ReaderOperations implements GraphQLQueryResolver, GraphQLMutationResolver {
    private final ReaderService readerService;

    public ReaderOperations(ReaderService readerService) {
        this.readerService = readerService;
    }

    public ReaderPage readers(ReaderFilter filter, PageQuery page) {
        return readerService.search(filter, page);
    }

    public Reader reader(Integer id) {
        return readerService.get(id);
    }

    public Reader createReader(ReaderInput input) {
        return readerService.create(input);
    }

    public Reader updateReader(ReaderUpdateInput input) {
        return readerService.update(input);
    }

    public boolean deleteReader(Integer id) {
        return readerService.delete(id);
    }
}
