package com.allst.boot.subscription.service;

import com.allst.boot.subscription.graphql.input.CategoryInput;
import com.allst.boot.subscription.graphql.input.CategoryUpdateInput;
import com.allst.boot.subscription.model.Category;
import com.allst.boot.subscription.repository.BookCategoryRepository;
import com.allst.boot.subscription.repository.CategoryRepository;
import com.allst.boot.subscription.support.BusinessException;
import com.allst.boot.subscription.support.Texts;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;

    public CategoryService(CategoryRepository categoryRepository, BookCategoryRepository bookCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.bookCategoryRepository = bookCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> list() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public Category get(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "分类不存在"));
    }

    @Transactional
    public Category create(CategoryInput input) {
        String name = normalizeName(input == null ? null : input.getName());
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException("CONFLICT", "分类名称已存在");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(Texts.trimToNull(input.getDescription()));
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(CategoryUpdateInput input) {
        if (input == null || input.getId() == null) {
            throw new BusinessException("BAD_REQUEST", "缺少分类编号");
        }
        Category category = get(input.getId());
        if (input.getName() != null) {
            String name = normalizeName(input.getName());
            if (categoryRepository.existsByNameAndIdNot(name, category.getId())) {
                throw new BusinessException("CONFLICT", "分类名称已存在");
            }
            category.setName(name);
        }
        if (input.getDescription() != null) {
            category.setDescription(Texts.trimToNull(input.getDescription()));
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public boolean delete(Integer id) {
        Category category = get(id);
        if (bookCategoryRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("REFERENCED", "分类下还有图书，先移出分类再删除");
        }
        categoryRepository.delete(category);
        return true;
    }

    private String normalizeName(String name) {
        String normalized = Texts.require(name, "分类名称不能为空");
        if (normalized.length() > 32) {
            throw new BusinessException("BAD_REQUEST", "分类名称不能超过 32 个字");
        }
        return normalized;
    }
}
