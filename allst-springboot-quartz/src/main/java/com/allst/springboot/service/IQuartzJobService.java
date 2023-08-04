package com.allst.springboot.service;

import com.allst.springboot.entity.QuartzJob;
import org.quartz.SchedulerException;

/**
 * @author Hutu
 * @since 2023-05-30 下午 09:43
 */
public interface IQuartzJobService {
    /**
     * 暂停任务
     *
     * @param job 调度信息
     */
    int pauseJob(QuartzJob job) throws SchedulerException;

    /**
     * 恢复任务
     *
     * @param job 调度信息
     */
    int resumeJob(QuartzJob job) throws SchedulerException;

    /**
     * 删除任务后，所对应的trigger也将被删除
     *
     * @param job 调度信息
     */
    int deleteJob(QuartzJob job) throws SchedulerException;

    /**
     * 任务调度状态修改
     */
    int changeStatus(Long jobId, String status) throws SchedulerException;

    /**
     * 立即运行任务
     *
     * @param job 调度信息
     */
    void run(QuartzJob job) throws SchedulerException;

    /**
     * 新增任务
     *
     * @param job 调度信息
     */
    int insertJob(QuartzJob job) throws Exception;

    /**
     * 更新任务
     *
     * @param job 调度信息
     */
    int updateJob(QuartzJob job) throws SchedulerException, Exception;

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression 表达式
     */
    boolean checkCronExpressionIsValid(String cronExpression);
}
