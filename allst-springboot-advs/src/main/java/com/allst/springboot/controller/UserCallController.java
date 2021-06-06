package com.allst.springboot.controller;

import com.allst.springboot.entity.Result;
import com.allst.springboot.entity.User;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * @author June
 * @since 2021年06月
 */
@RestController
public class UserCallController {

    /*
     * 声明RestTemplate
     */
    private final RestTemplate restTemplate;
    /*
     * 当bean没有无参构造时， spring将自动拿有参构造器， 参数进行自动注入
     */
    public UserCallController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Bean
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        messageConverters.add(converter);
        restTemplate.setMessageConverters(messageConverters);

        return restTemplate;
    }

    @RequestMapping("/userCall")
    public String userCall() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_HTML, MediaType.ALL));
        restTemplate.getMessageConverters().add(mappingJackson2HttpMessageConverter);
        Result forObject = restTemplate.getForObject("http://localhost:8027/basic/user/{id}", Result.class, 1);
        assert forObject != null;
        System.out.println(forObject.toString());
        return forObject.toString();
    }

    @RequestMapping("/updateUserCall")
    public String updateUserCall() {
        Result forObject = restTemplate.getForObject("http://localhost:8027/basic/user/{id}", Result.class, 1);
        assert forObject != null;
        System.out.println(forObject.toString());
        return forObject.toString();
    }

    @RequestMapping("/str/userCall")
    public String strUserCall() {
        String forObject = restTemplate.getForObject("http://localhost:8027/basic/user/str/{id}", String.class, 1);
        assert forObject != null;
        return forObject;
    }


    @RequestMapping("/userCall/str")
    public String userStringCall() {
        ResponseEntity<User> userResponseEntity = restTemplate.getForEntity("http://localhost:8027/basic/user/user/{id}", User.class, 3);
        System.out.println("Msg: " + userResponseEntity.getStatusCode());
        return Objects.requireNonNull(userResponseEntity.getBody()).toString();
    }
}
