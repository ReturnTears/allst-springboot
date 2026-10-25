package com.allst.netty.tcp;

import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.TimeUnit;

/**
 * 统一关闭 Netty 线程组，避免各生命周期类各自写一遍等待时间。
 */
public final class NettyThreads {

    private NettyThreads() {
    }

    public static void shutdown(EventExecutorGroup group) {
        if (group == null) {
            return;
        }
        group.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
    }
}
