package com.allst.exercise.model;

import com.mybatisflex.annotation.Table;

/**
 * @author Hutu
 * @since 2024-07-28 下午 02:16
 */
@Table("person")
public class Person {
    private Long id;
    private String firstName;
    private String lastName;
    private Integer age;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
