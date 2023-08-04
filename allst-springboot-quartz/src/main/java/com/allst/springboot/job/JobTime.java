package com.allst.springboot.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author Hutu
 * @since 2023-05-29 下午 10:33
 */
public class JobTime implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println(context.getTrigger().getStartTime());
        System.out.println("start exec job ... ");
        System.out.println(context.getTrigger().getEndTime());
    }
}
