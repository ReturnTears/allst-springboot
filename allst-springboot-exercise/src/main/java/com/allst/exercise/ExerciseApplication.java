package com.allst.exercise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.allst.exercise.mapper")
public class ExerciseApplication {

	public static void main(String[] args) {
		System.out.println("Halo World !");
		SpringApplication.run(ExerciseApplication.class, args);
	}

}
