package com.allst.exercise.controller;

import com.allst.exercise.common.Result;
import com.allst.exercise.model.dto.PersonDto;
import com.allst.exercise.service.PersonService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-07-28 下午 02:33
 */
@RestController
@RequestMapping("/person")
@Api(tags = "Person控制器")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping("/selectBy/{id}")
    public Result<PersonDto> selectById(@PathVariable(value = "id") Long id) {
        return Result.ok(personService.selectById(id));
    }

}
