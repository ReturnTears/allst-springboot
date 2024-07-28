package com.allst.exercise.service;

import com.allst.exercise.model.dto.PersonDto;

/**
 * @author Hutu
 * @since 2024-07-28 下午 02:34
 */
public interface PersonService {
    PersonDto selectById(Long id);
}
