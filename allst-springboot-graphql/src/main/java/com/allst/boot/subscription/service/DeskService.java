package com.allst.boot.subscription.service;

import com.allst.boot.repository.AuthorRepository;
import com.allst.boot.repository.BookRepository;
import com.allst.boot.subscription.graphql.dto.DeskStats;
import com.allst.boot.subscription.model.MembershipStatus;
import com.allst.boot.subscription.repository.CategoryRepository;
import com.allst.boot.subscription.repository.MembershipRepository;
import com.allst.boot.subscription.repository.ReaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeskService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final ReaderRepository readerRepository;
    private final CategoryRepository categoryRepository;
    private final MembershipRepository membershipRepository;

    public DeskService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       ReaderRepository readerRepository,
                       CategoryRepository categoryRepository,
                       MembershipRepository membershipRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.readerRepository = readerRepository;
        this.categoryRepository = categoryRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public DeskStats stats() {
        return new DeskStats(
                (int) bookRepository.count(),
                (int) authorRepository.count(),
                (int) readerRepository.count(),
                (int) categoryRepository.count(),
                (int) membershipRepository.countByStatus(MembershipStatus.ACTIVE)
        );
    }
}
