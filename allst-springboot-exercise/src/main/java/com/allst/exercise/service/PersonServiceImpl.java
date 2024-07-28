package com.allst.exercise.service;

import com.allst.exercise.mapper.PersonMapper;
import com.allst.exercise.model.Person;
import com.allst.exercise.model.dto.PersonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Hutu
 * @since 2024-07-28 下午 02:34
 */
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonMapper personMapper;
    @Override
    public PersonDto selectById(Long id) {
        Person person = personMapper.selectById(id);
        System.out.println("person = " + person);
        return com.allst.exercise.do2dto.PersonMapper.INSTANCE.personToPersonDto(person);
    }
}
