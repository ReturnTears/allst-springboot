package com.allst.springboot.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

/**
 * 多次调用 Job 的时候，将参数保留在 JobDataMap
 *
 * @author Hutu
 * @since 2023-05-29 下午 10:25
 */
@PersistJobDataAfterExecution
public class JobStatus implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long count = (long) context.getJobDetail().getJobDataMap().get("count");
        System.out.println("current exec is : " + count + " times......");
        context.getJobDetail().getJobDataMap().put("count", ++count);
    }
}
