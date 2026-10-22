package com.allst.springboot.common.exception;

import com.allst.springboot.common.api.Result;
import com.allst.springboot.common.api.ResultCode;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * 没有进入 {@link GlobalExceptionHandler} 的容器错误（例如找不到接口）也返回统一结构。
 */
@RestController
public class ResultErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Result<Void>> error(HttpServletRequest request) {
        Object statusValue = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusValue instanceof Integer ? (Integer) statusValue : 500;
        ResultCode resultCode = map(status);
        HttpStatus httpStatus = HttpStatus.resolve(status);
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(httpStatus).body(Result.fail(resultCode));
    }

    private ResultCode map(int status) {
        if (status == 404) {
            return ResultCode.API_NOT_FOUND;
        }
        if (status == 405) {
            return ResultCode.METHOD_NOT_ALLOWED;
        }
        if (status == 400) {
            return ResultCode.BAD_REQUEST;
        }
        return ResultCode.INTERNAL_ERROR;
    }
}
