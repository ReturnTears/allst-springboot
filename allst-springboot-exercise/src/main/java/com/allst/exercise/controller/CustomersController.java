package com.allst.exercise.controller;

import com.allst.exercise.anno.CustomersFormat;
import com.allst.exercise.model.Customers;
import com.allst.exercise.service.CustomersService;
import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-07-27 下午 11:50
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomersController {

    private final CustomersService customersService;

    /**
     * <a href="http://127.0.0.1:1024/customer/save?customers=2,Xiaohu,xiaohu@qq.com,guangzhou,guangzhou,china">请求方式1</a>
     * <a href="http://127.0.0.1:1024/customer/save?id=4&name=XiaoQ&email=3_xiaoshen&address=shenzhen&city=shenzhen&country=china">请求方式2</a>
     */
    @GetMapping("/save")
    public Object save(Customers customers) {
        return customersService.save(customers);
    }

    /**
     * 注解添加到参数上
     * <a href="http://127.0.0.1:1024/customer/save?id=7&name=Seven&email=Seven@qq.com&address=changan&city=xian&country=china">请求方式3</a>
     */
    @GetMapping("/saveBy")
    public Object saveBy(@CustomersFormat Customers customers) {
        return customersService.save(customers);
    }

    @GetMapping("/query/{id}")
    public Object queryOne(@PathVariable(value = "id") Long id) {
        return customersService.queryOne(id);
    }

    @GetMapping("/select/{id}")
    public Customers selectOne(@PathVariable(value = "id") Long id) {
        return customersService.selectById(id);
    }

    /**
     * <a href="http://127.0.0.1:1024/customer/select_name/Xiaohu">请求地址</a>
     */
    @GetMapping("/select_name/{name}")
    public Customers selectName(@PathVariable(value = "name") String name) {
        return customersService.selectByName(name);
    }

    @GetMapping("/page_list/{pageSize}/{pageNumber}")
    public Page<Customers> selectPageList(@PathVariable(value = "pageSize") Integer pageSize, @PathVariable(value = "pageNumber") Integer pageNumber) {
        return customersService.selectPageList(pageSize, pageNumber);
    }
}
