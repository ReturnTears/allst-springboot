package com.allst.springboot.extension.lifecycle;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 命令行一次性任务入口。与 ApplicationRunner 按 @Order 放在同一个队列里。
 */
@Component
@Order(20)
public class AdvsCommandJobRunner implements CommandLineRunner {

    private final ApplicationArguments applicationArguments;

    public AdvsCommandJobRunner(ApplicationArguments applicationArguments) {
        this.applicationArguments = applicationArguments;
    }

    @Override
    public void run(String... args) {
        List<String> jobs = applicationArguments.getOptionValues("advs.job");
        if (jobs == null || jobs.isEmpty()) {
            StartupTraceLog.record(StartupPhase.COMMAND_LINE_RUNNER,
                    "没有 --advs.job，命令行任务保持空闲。可选值 extension-report。");
            return;
        }
        for (String job : jobs) {
            if ("extension-report".equals(job)) {
                StartupTraceLog.record(StartupPhase.COMMAND_LINE_RUNNER,
                        "执行 extension-report，当前轨迹 " + StartupTraceLog.size() + " 条。");
            } else {
                StartupTraceLog.record(StartupPhase.COMMAND_LINE_RUNNER,
                        "忽略未知任务 " + job + "。目前只识别 extension-report。");
            }
        }
    }
}
