package com.allst.springboot.service;

import com.allst.springboot.bean.Department;

/**
 * @author YiYa
 * @since 2020-08-23 下午 03:39
 */
public interface DepartmentService {

    Department getDepartmentWithCacheById(Integer id);

}
