package com.allst.exercise.controller;

import com.allst.exercise.model.dto.PersonDto;
import com.allst.exercise.service.PersonService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-07-28 下午 02:33
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/person")
@Api(tags = "Person控制器")
public class PersonController {

    private final PersonService personService;
    @GetMapping("/selectBy/{id}")
    public PersonDto selectById(@PathVariable(value = "id") Long id) {
        return personService.selectById(id);
    }

}
