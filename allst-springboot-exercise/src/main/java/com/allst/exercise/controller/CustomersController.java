package com.allst.exercise.controller;

import com.allst.exercise.anno.CustomersFormat;
import com.allst.exercise.common.Result;
import com.allst.exercise.model.Customers;
import com.allst.exercise.service.CustomersService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Hutu
 * @since 2024-07-27 下午 11:50
 */
@RestController
@RequestMapping("/customer")
@Api(tags = "Customers控制器")
public class CustomersController {

    private final CustomersService customersService;

    public CustomersController(CustomersService customersService) {
        this.customersService = customersService;
    }

    /**
     * <a href="http://127.0.0.1:1024/customer/save?customers=2,Xiaohu,xiaohu@qq.com,guangzhou,guangzhou,china">请求方式1</a>
     * <a href="http://127.0.0.1:1024/customer/save?id=4&name=XiaoQ&email=3_xiaoshen&address=shenzhen&city=shenzhen&country=china">请求方式2</a>
     */
    @GetMapping("/save")
    public Result<Object> save(Customers customers) {
        customersService.save(customers);
        return Result.ok(customers);
    }

    /**
     * 注解添加到参数上
     * <a href="http://127.0.0.1:1024/customer/save?id=7&name=Seven&email=Seven@qq.com&address=changan&city=xian&country=china">请求方式3</a>
     */
    @GetMapping("/saveBy")
    public Result<Object> saveBy(@CustomersFormat Customers customers) {
        customersService.save(customers);
        return Result.ok(customers);
    }

    @GetMapping("/query/{id}")
    public Result<String> queryOne(@PathVariable(value = "id") Long id) {
        return Result.ok(customersService.queryOne(id));
    }

    @GetMapping("/select/{id}")
    public Result<Customers> selectOne(@PathVariable(value = "id") Long id) {
        return Result.ok(customersService.selectById(id));
    }

    /**
     * <a href="http://127.0.0.1:1024/customer/select_name/Xiaohu">请求地址</a>
     */
    @GetMapping("/select_name/{name}")
    public Result<Customers> selectName(@PathVariable(value = "name") String name) {
        return Result.ok(customersService.selectByName(name));
    }

    @GetMapping("/page_list/{pageSize}/{pageNumber}")
    public Result<Page<Customers>> selectPageList(@PathVariable(value = "pageSize") Integer pageSize, @PathVariable(value = "pageNumber") Integer pageNumber) {
        return Result.ok(customersService.selectPageList(pageSize, pageNumber));
    }

    @GetMapping("/page_cursor_list")
    public Result<List<Customers>> selectCursorList() {
        return Result.ok(customersService.selectCursorList());
    }
}
