package com.allst.boot.subscription.graphql;

import com.allst.boot.model.Book;
import com.allst.boot.repository.BookRepository;
import com.allst.boot.subscription.model.BookCategory;
import com.allst.boot.subscription.model.Category;
import com.allst.boot.subscription.model.Membership;
import com.allst.boot.subscription.model.Reader;
import com.allst.boot.subscription.repository.BookCategoryRepository;
import com.allst.boot.subscription.repository.CategoryRepository;
import com.allst.boot.subscription.repository.MembershipRepository;
import com.allst.boot.subscription.repository.ReaderRepository;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LoaderRegistryFactory {
    private final ReaderRepository readerRepository;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final MembershipRepository membershipRepository;

    public LoaderRegistryFactory(ReaderRepository readerRepository,
                                 BookRepository bookRepository,
                                 CategoryRepository categoryRepository,
                                 BookCategoryRepository bookCategoryRepository,
                                 MembershipRepository membershipRepository) {
        this.readerRepository = readerRepository;
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.bookCategoryRepository = bookCategoryRepository;
        this.membershipRepository = membershipRepository;
    }

    public DataLoaderRegistry create() {
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register(LoaderNames.READER_BY_ID, DataLoaderFactory.newMappedDataLoader(this::loadReaders));
        registry.register(LoaderNames.BOOK_BY_ID, DataLoaderFactory.newMappedDataLoader(this::loadBooks));
        registry.register(LoaderNames.CATEGORY_BY_BOOK_ID, DataLoaderFactory.newMappedDataLoader(this::loadCategoriesByBook));
        registry.register(LoaderNames.BOOK_COUNT_BY_CATEGORY_ID, DataLoaderFactory.newMappedDataLoader(this::loadBookCounts));
        registry.register(LoaderNames.MEMBERSHIPS_BY_READER_ID, DataLoaderFactory.newMappedDataLoader(this::loadMemberships));
        return registry;
    }

    private CompletableFuture<Map<Integer, Reader>> loadReaders(Set<Integer> keys) {
        return CompletableFuture.completedFuture(mapById(readerRepository.findAllById(keys), Reader::getId));
    }

    private CompletableFuture<Map<Integer, Book>> loadBooks(Set<Integer> keys) {
        return CompletableFuture.completedFuture(mapById(bookRepository.findAllById(keys), Book::getId));
    }

    private CompletableFuture<Map<Integer, Category>> loadCategoriesByBook(Set<Integer> bookIds) {
        List<BookCategory> links = bookCategoryRepository.findByBookIdIn(bookIds);
        Set<Integer> categoryIds = links.stream().map(BookCategory::getCategoryId).collect(Collectors.toSet());
        Map<Integer, Category> categories = mapById(categoryRepository.findAllById(categoryIds), Category::getId);
        Map<Integer, Category> result = new HashMap<>();
        for (BookCategory link : links) {
            Category category = categories.get(link.getCategoryId());
            if (category != null) {
                result.put(link.getBookId(), category);
            }
        }
        return CompletableFuture.completedFuture(result);
    }

    private CompletableFuture<Map<Integer, Integer>> loadBookCounts(Set<Integer> categoryIds) {
        Map<Integer, Integer> counts = new HashMap<>();
        categoryIds.forEach(id -> counts.put(id, 0));
        for (BookCategory link : bookCategoryRepository.findByCategoryIdIn(categoryIds)) {
            counts.merge(link.getCategoryId(), 1, Integer::sum);
        }
        return CompletableFuture.completedFuture(counts);
    }

    private CompletableFuture<Map<Integer, List<Membership>>> loadMemberships(Set<Integer> readerIds) {
        List<Membership> memberships = new ArrayList<>(membershipRepository.findByReaderIdIn(readerIds));
        memberships.sort(Comparator.comparing(Membership::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        Map<Integer, List<Membership>> grouped = memberships.stream()
                .collect(Collectors.groupingBy(Membership::getReaderId));
        readerIds.forEach(id -> grouped.putIfAbsent(id, List.of()));
        return CompletableFuture.completedFuture(grouped);
    }

    private <T> Map<Integer, T> mapById(Iterable<T> items, Function<T, Integer> idGetter) {
        Map<Integer, T> result = new HashMap<>();
        for (T item : items) {
            result.put(idGetter.apply(item), item);
        }
        return result;
    }
}
