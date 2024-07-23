package com.allst.springboot.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 定时任务Service
 *
 * @author YiYa
 * @since 2020-10-13 上午 12:22
 */
@Service
public class ScheduledService {
    /**
     * second,minute,hour,day of month, month, day of week
     */
    // @Scheduled(cron = "0 * * * * MON-FRI") // （任意月任意天的）周一到周五任意时任意分的第一秒执行
    // @Scheduled(cron = "0,1,2,3,4 * * * * MON-FRI")  // 第1,2,3,4秒执行
    // @Scheduled(cron = "0-4 * * * * MON-FRI")  // 第1-4秒执行 效果同上
    // @Scheduled(cron = "0/4 * * * * MON-FRI")  // 第0秒启动 每4秒执行一次
    @Scheduled(cron = "0 0/10 0/1 * * ?")  // 第0秒启动 每4秒执行一次
    public void scheduledHandleTask() {
        System.out.println("-----scheduledHandleTask-----");
    }
}
