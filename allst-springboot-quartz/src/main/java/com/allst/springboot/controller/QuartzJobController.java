package com.allst.springboot.controller;

import com.allst.springboot.core.BaseController;
import com.allst.springboot.core.Constants;
import com.allst.springboot.entity.QuartzJob;
import com.allst.springboot.service.IQuartzJobService;
import com.allst.springboot.util.ApiResp;
import com.allst.springboot.util.CronUtils;
import com.allst.springboot.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Hutu
 * @since 2023-05-30 下午 10:09
 */
@Api(tags = "Quartz 任务调度接口")
@RestController
@RequestMapping("/quartz/job")
public class QuartzJobController extends BaseController {
    @Autowired
    private IQuartzJobService quartzJobService;

    @ApiOperation(value = "新增task任务", notes = "将任务信息添加到数据库")
    @PostMapping(value = "add")
    public ApiResp<QuartzJob> add(@RequestBody QuartzJob job) throws Exception {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return ApiResp.<QuartzJob>builder().code(error).msg("新增任务'" + job.getJobName() + "'失败，Cron表达式不正确").build();
        } else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_RMI)) {
            return ApiResp.<QuartzJob>builder().code(error).msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi://'调用").build();
        } else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_LDAP)) {
            return ApiResp.<QuartzJob>builder().code(error).msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap://'调用").build();
        }
        quartzJobService.insertJob(job);
        return ApiResp.<QuartzJob>builder().code(success).build();
    }

    @ApiOperation(value = "根据任务ID修改状态，0启动 1暂停")
    @PostMapping(value = "changeStatus")
    public ApiResp<Object> changeStatus(Long jobId, String status) throws SchedulerException {
        quartzJobService.changeStatus(jobId, status);
        return ApiResp.builder().code(success).build();
    }


    @ApiOperation(value = "修改task任务", notes = "根据ID修改已有的任务，同时更新调度任务信息")
    @PostMapping(value = "update")
    public ApiResp<QuartzJob> update(@RequestBody QuartzJob job) throws Exception {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return ApiResp.<QuartzJob>builder().code(error).msg("修改任务'" + job.getJobName() + "'失败，Cron表达式不正确").build();
        } else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_RMI)) {
            return ApiResp.<QuartzJob>builder().code(error).msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi://'调用").build();
        } else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_LDAP)) {
            return ApiResp.<QuartzJob>builder().code(error).msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap://'调用").build();
        }
        quartzJobService.updateJob(job);
        return ApiResp.<QuartzJob>builder().code(success).build();
    }

    @GetMapping("/handle")
    public String handleTask() {
        return "handleTask ~";
    }
}
