package com.allst.exercise.do2dto;

import com.allst.exercise.model.Person;
import com.allst.exercise.model.dto.PersonDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * @author Hutu
 * @since 2024-07-28 上午 12:02
 */
@Mapper
public interface PersonMapper {
    PersonMapper INSTANCE = Mappers.getMapper(PersonMapper.class);

    default String concatenateNames(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(target = "fullName", expression = "java(concatenateNames(person.getFirstName(), person.getLastName()))"),
            @Mapping(source = "age", target = "age")
    })
    PersonDto personToPersonDto(Person person);
}
