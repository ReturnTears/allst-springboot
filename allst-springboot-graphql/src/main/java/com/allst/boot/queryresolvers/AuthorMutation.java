package com.allst.boot.queryresolvers;

import com.allst.boot.model.Author;
import com.allst.boot.repository.AuthorRepository;
import com.allst.boot.service.AuthorService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hutu
 * @since 2024-07-30 下午 10:57
 */
@Component
public class AuthorMutation implements GraphQLMutationResolver {
    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorService authorService;

    @Transactional
    public Author updateAuthor(Integer id, String firstName) {
        authorRepository.updateAuthor(id, firstName);
        return new Author(id, firstName);
    }

    public Author updateAuthorById(Integer id, String firstName, String lastName) {
        Author author = new Author(id, firstName, lastName);
        authorService.updateAuthorById(id, author);
        return author;
    }
}
