package com.allst.springboot.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author YiYa
 * @since 2020-08-22 下午 11:00
 */
@RestController
public class JDBCController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/jdbc")
    public Map<String, Object> map() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from sys_def");
        return list.get(0);
    }
}
