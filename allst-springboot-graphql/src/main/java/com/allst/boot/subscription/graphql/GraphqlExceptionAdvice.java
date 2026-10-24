package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.support.BusinessException;
import graphql.GraphQLError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;

@ControllerAdvice
public class GraphqlExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(GraphqlExceptionAdvice.class);

    @ExceptionHandler(BusinessException.class)
    public GraphQLError business(BusinessException exception) {
        return new CodedGraphqlError(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public GraphQLError missing(EntityNotFoundException exception) {
        return new CodedGraphqlError("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public GraphQLError integrity(DataIntegrityViolationException exception) {
        log.warn("数据约束冲突: {}", exception.getMostSpecificCause().getMessage());
        return new CodedGraphqlError("REFERENCED", "记录与已有数据冲突，可能是名称重复，或仍被其他数据引用");
    }

    @ExceptionHandler(Exception.class)
    public GraphQLError fallback(Exception exception) {
        log.error("GraphQL 字段执行失败", exception);
        return new CodedGraphqlError("INTERNAL", "服务处理失败");
    }
}
