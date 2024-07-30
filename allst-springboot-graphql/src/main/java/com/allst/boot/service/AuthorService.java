package com.allst.boot.service;

import com.allst.boot.model.Author;
import com.allst.boot.repository.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityNotFoundException;

/**
 * @author Hutu
 * @since 2024-07-30 下午 10:59
 */
@Service
public class AuthorService {
    private final AuthorRepository authorRepository;

    private final PlatformTransactionManager transactionManager;

    @Autowired
    public AuthorService(AuthorRepository authorRepository, PlatformTransactionManager transactionManager) {
        this.authorRepository = authorRepository;
        this.transactionManager = transactionManager;
    }

    public Author updateAuthorById(Integer id, Author updatedAuthor) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute((status) -> authorRepository.findById(id)
                .map(author -> {
                    author.setFirstName(updatedAuthor.getFirstName());
                    author.setLastName(updatedAuthor.getLastName());
                    return authorRepository.save(author);
                })
                .orElseThrow(() -> new EntityNotFoundException("Author not found with id " + id)));
    }
}
