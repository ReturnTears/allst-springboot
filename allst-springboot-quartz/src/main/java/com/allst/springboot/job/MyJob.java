package com.allst.springboot.job;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author Hutu
 * @since 2023-05-25 下午 10:15
 */
public class MyJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        System.out.println("jobDataMap : " + jobDataMap.toString());
        System.out.println("task has executed......");
    }
}
