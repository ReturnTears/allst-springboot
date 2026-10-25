package com.allst.springboot.common.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

/**
 * 把外部通知推迟到事务提交之后，再丢进有界线程池。
 * 事务回滚时不会触发 afterCommit，因此通知不会先于状态落库。
 */
@Component
public class AfterCommitExecutor {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitExecutor.class);

    private final Executor executor;
    private final AsyncTaskRecorder recorder;

    public AfterCommitExecutor(@Qualifier("stateMachineExecutor") Executor executor,
                               AsyncTaskRecorder recorder) {
        this.executor = executor;
        this.recorder = recorder;
    }

    public void execute(String taskName, Runnable task) {
        Runnable wrapped = () -> {
            try {
                task.run();
                recorder.markDone(taskName);
            } catch (RuntimeException exception) {
                recorder.markFailed(taskName);
                log.error("async task {} failed", taskName, exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(wrapped);
                }
            });
            return;
        }
        executor.execute(wrapped);
    }
}
