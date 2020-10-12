package com.allst.springboot.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步任务Service
 *
 * @author YiYa
 * @since 2020-10-13 上午 12:04
 */
@Service
public class AsyncService {

    /**
     * 未使用异步
     */
    public void noAsyncHandleTask() {
        try {
            // 模拟任务处理时长
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-----noAsyncHandleTask-----");
    }

    /**
     * 使用异步
     * 注解@Async 告诉Spring这是一个异步方法，Spring会自己开一个线程池来调用
     * 需要在启动类开启异步@EnableAsync
     */
    @Async
    public void inAsyncHandleTask() {
        try {
            // 模拟任务处理时长
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-----inAsyncHandleTask-----");
    }

}
