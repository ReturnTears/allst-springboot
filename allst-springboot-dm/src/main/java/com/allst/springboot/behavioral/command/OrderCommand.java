package com.allst.springboot.behavioral.command;

import java.util.Map;

/**
 * 命令（Command）——把一次业务动作封装成带身份、可重试、可撤销的对象。
 * <Pre>
 * 场景：支付回调、扣库存、发券这类动作会来自 HTTP、定时任务、消息消费者，
 * 需要统一的「重了怎么办、失败了怎么重试、要回滚怎么撤」。把它们都变成命令后，
 * 执行器只需对着接口编排，调用方不必各自写 try-catch 和去重。
 * <Pre>
 * id() 同时是幂等键；context 用来传递本次执行的可变数据。
 */
public interface OrderCommand {

    String id();

    int maxAttempts();

    String execute(Map<String, Object> context);

    void undo(Map<String, Object> context);
}
