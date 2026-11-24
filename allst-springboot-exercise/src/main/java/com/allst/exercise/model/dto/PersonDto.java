package com.allst.exercise.model.dto;

/**
 * @author Hutu
 * @since 2024-07-28 下午 02:16
 */
public class PersonDto {
    private Long id;
    private String fullName;
    private Integer age;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
