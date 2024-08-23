package com.allst.springboot.controller;

import com.allst.springboot.base.ApiResponse;
import com.allst.springboot.base.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-08-23 下午 10:44
 */
@RestController
@RequestMapping("/halo")
public class HaloController extends BaseController {

    @RequestMapping("/get")
    public ResponseEntity<ApiResponse<String>> get() {
        return success(new ApiResponse<>(200, "Success", "Halo World!"));
    }

    @RequestMapping("/del")
    public ResponseEntity<ApiResponse<String>> del() {
        return success(new ApiResponse<>(200, "Delete Success"));
    }
}
