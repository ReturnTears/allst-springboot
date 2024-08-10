package com.allst.boot.listener;

import com.allst.boot.model.BinLogItem;

/**
 * @author Hutu
 * @since 2024-08-10 上午 11:14
 */
@FunctionalInterface
public interface BinLogListener {
    void onEvent(BinLogItem item);
}
