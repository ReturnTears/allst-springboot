package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.model.Category;
import com.allst.boot.subscription.service.CategoryService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.kickstart.tools.GraphQLQueryResolver;
import com.allst.boot.subscription.graphql.input.CategoryInput;
import com.allst.boot.subscription.graphql.input.CategoryUpdateInput;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryOperations implements GraphQLQueryResolver, GraphQLMutationResolver {
    private final CategoryService categoryService;

    public CategoryOperations(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public List<Category> categories() {
        return categoryService.list();
    }

    public Category category(Integer id) {
        return categoryService.get(id);
    }

    public Category createCategory(CategoryInput input) {
        return categoryService.create(input);
    }

    public Category updateCategory(CategoryUpdateInput input) {
        return categoryService.update(input);
    }

    public boolean deleteCategory(Integer id) {
        return categoryService.delete(id);
    }
}
