package com.allst.springboot.entity;

import com.allst.springboot.core.ScheduleConstants;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 定时任务调度表
 *
 * @author Hutu
 * @since 2023-05-30 下午 09:36
 */
@Data
@ApiModel
public class QuartzJob implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "任务序号")
    @TableId
    private Long jobId;

    @ApiModelProperty(value = "任务名称")
    private String jobName;

    @ApiModelProperty(value = "任务组名")
    private String jobGroup;

    @ApiModelProperty(value = "调用目标字符串")
    private String invokeTarget;

    @ApiModelProperty(value = "执行表达式")
    private String cronExpression;

    @ApiModelProperty(value = "cron计划策略", notes = "0=默认,1=立即触发执行,2=触发一次执行,3=不触发立即执行")
    private String misfirePolicy = ScheduleConstants.MISFIRE_DEFAULT;

    @ApiModelProperty(value = "并发执行", notes = "0=允许,1=禁止")
    private String concurrent;

    @ApiModelProperty(value = "任务状态（0正常 1暂停）")
    private String status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
