package com.allst.springboot.aspect;

import com.alibaba.fastjson2.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 定义Demo切面
 *
 * @author Hutu
 * @since 2024-08-28 下午 09:51
 */
@Aspect
@Component
public class DemoAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoAspect.class);
    @Pointcut("@annotation(com.allst.springboot.anno.OnChange)")
    private void onChange() {}

    @Around("onChange()")
    public Object handleChange(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.info("come in method DemoAspect.handleChange");
        Object[] args = joinPoint.getArgs();
        // 执行原方法
        Object result = joinPoint.proceed();
        LOGGER.info("exec method result : " + JSON.toJSONString(args));
        return result;
    }
}
