package com.allst.boot.subscription.service;

import com.allst.boot.model.Book;
import com.allst.boot.repository.BookRepository;
import com.allst.boot.subscription.graphql.input.BookUpdateInput;
import com.allst.boot.subscription.model.BookCategory;
import com.allst.boot.subscription.repository.BookCategoryRepository;
import com.allst.boot.subscription.repository.CategoryRepository;
import com.allst.boot.subscription.support.BusinessException;
import com.allst.boot.subscription.support.Texts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CatalogService {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;

    public CatalogService(BookRepository bookRepository,
                          CategoryRepository categoryRepository,
                          BookCategoryRepository bookCategoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.bookCategoryRepository = bookCategoryRepository;
    }

    @Transactional(readOnly = true)
    public Book get(Integer id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "图书不存在"));
    }

    @Transactional(readOnly = true)
    public List<Book> search(String keyword) {
        List<Book> books = StreamSupport.stream(bookRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(Book::getId, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        String needle = Texts.trimToNull(keyword);
        if (needle == null) {
            return books;
        }
        String lowered = needle.toLowerCase();
        return books.stream()
                .filter(book -> book.getName() != null && book.getName().toLowerCase().contains(lowered))
                .collect(Collectors.toList());
    }

    @Transactional
    public Book update(BookUpdateInput input) {
        if (input == null || input.getId() == null) {
            throw new BusinessException("BAD_REQUEST", "缺少图书编号");
        }
        Book book = get(input.getId());
        if (input.getName() != null) {
            String name = Texts.require(input.getName(), "书名不能为空");
            Book sameName = bookRepository.findBookByName(name);
            if (sameName != null && !sameName.getId().equals(book.getId())) {
                throw new BusinessException("CONFLICT", "书名已存在");
            }
            book.setName(name);
        }
        if (input.getPageCount() != null) {
            book.setPageCount(input.getPageCount().trim());
        }
        return bookRepository.save(book);
    }

    @Transactional
    public Book assignCategory(Integer bookId, Integer categoryId) {
        Book book = get(bookId);
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException("NOT_FOUND", "分类不存在");
        }
        BookCategory link = bookCategoryRepository.findByBookId(bookId).orElseGet(BookCategory::new);
        link.setBookId(bookId);
        link.setCategoryId(categoryId);
        bookCategoryRepository.save(link);
        return book;
    }

    @Transactional
    public boolean clearCategory(Integer bookId) {
        get(bookId);
        bookCategoryRepository.deleteByBookId(bookId);
        return true;
    }
}
