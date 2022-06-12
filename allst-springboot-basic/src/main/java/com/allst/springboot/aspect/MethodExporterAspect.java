package com.allst.springboot.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 切面类，用于增强其他方法
 *
 * @author June
 * @since 2022-06-12
 */
@Aspect // 表示当前对象是一个切面,
@Component // 表示允许在Spring IOC中对当前对象实例化并管理
@Slf4j
public class MethodExporterAspect {
    /**
     * 说明切面的作用范围，任何增加@methodExporter注解的目标方法都将在执行方法前执行该切面方法
     *
     * @return 结果
     *
     * @Around注解 表示环绕通知，最强大的通知类型。可以控制方法入参，执行，返回结果等各个方面细节
     */
    @Around("@annotation(com.allst.springboot.annoation.MethodExporter)")
    public Object methodExporter(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("come in method :　methodExporter　。。。。。。");
        long st = new Date().getTime();
        // 执行目标方法，获取方法返回值
        Object proceed = joinPoint.proceed();

        long et = new Date().getTime();

        ObjectMapper mapper = new ObjectMapper();
        // 将入参JSON序列化
        String jsonParam = mapper.writeValueAsString(joinPoint.getArgs());

        String jsonResult = null;
        if (proceed != null) {
            jsonResult = mapper.writeValueAsString(proceed);
        } else {
            jsonResult = "result";
        }

        // 模拟上报过程
        log.info("正在上报服务器调用过程：target:{}.{}() execution:{} ms, paramter:{}, result:{}"
                , joinPoint.getTarget().getClass().getSimpleName()
                , joinPoint.getSignature().getName()
                , (et - st)
                , jsonParam
                , jsonResult
        );

        return proceed;
    }
}
