package com.allst.boot.subscription.support;

import com.allst.boot.subscription.graphql.dto.PageInfo;
import com.allst.boot.subscription.graphql.input.PageQuery;
import com.allst.boot.subscription.graphql.input.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageRequests {
    private PageRequests() {
    }

    public static Pageable of(PageQuery query, String defaultSort, Set<String> allowed) {
        int page = 1;
        int size = 10;
        String sortBy = defaultSort;
        Sort.Direction direction = Sort.Direction.DESC;
        if (query != null) {
            if (query.getPage() != null && query.getPage() > 0) {
                page = query.getPage();
            }
            if (query.getSize() != null && query.getSize() > 0) {
                size = Math.min(query.getSize(), 50);
            }
            if (query.getSortBy() != null && allowed.contains(query.getSortBy())) {
                sortBy = query.getSortBy();
            }
            if (query.getDirection() == SortDirection.ASC) {
                direction = Sort.Direction.ASC;
            }
        }
        return PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
    }

    public static PageInfo info(Page<?> page) {
        return new PageInfo(page.getNumber() + 1, page.getSize(), (int) page.getTotalElements(), page.getTotalPages());
    }
}
