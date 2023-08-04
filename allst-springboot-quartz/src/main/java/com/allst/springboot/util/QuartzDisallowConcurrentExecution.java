package com.allst.springboot.util;

import com.allst.springboot.entity.QuartzJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * 定时任务处理（禁止并发执行）
 *
 * @author Hutu
 * @since 2023-05-30 下午 09:59
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution  extends AbstractQuartzJob {
    @Override
    protected void doExecute(JobExecutionContext context, QuartzJob job) throws Exception {
        JobInvokeUtil.invokeMethod(job);
    }
}
