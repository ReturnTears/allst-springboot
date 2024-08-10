package com.allst.boot.listener;

import cn.hutool.core.collection.CollectionUtil;
import com.allst.boot.config.BinLogConfig;
import com.allst.boot.model.Conf;
import com.allst.boot.util.BinLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Hutu
 * @since 2024-08-10 上午 11:27
 */
@Slf4j
@Component
@Order(value = 1)
public class TourBinLogListener implements CommandLineRunner {
    @Resource
    private BinLogConfig binLogConstants;

    @Override
    public void run(String... args) throws Exception {
        log.info("初始化配置信息：" + binLogConstants.toString());

        // 初始化配置信息
        Conf conf = new Conf(binLogConstants.getHost(), binLogConstants.getPort(), binLogConstants.getUsername(), binLogConstants.getPasswd());

        // 初始化监听器
        MysqlBinLogListener mysqlBinLogListener = new MysqlBinLogListener(conf);

        // 获取table集合
        List<String> tableList = BinLogUtils.getListByStr(binLogConstants.getTable());
        if (CollectionUtil.isEmpty(tableList)) {
            return;
        }
        // 注册监听
        tableList.forEach(table -> {
            log.info("注册监听信息，注册DB：" + binLogConstants.getDb() + "，注册表：" + table);
            try {
                mysqlBinLogListener.regListener(binLogConstants.getDb(), table, item -> {
                    log.info("监听逻辑处理");
                });
            } catch (Exception e) {
                log.error("BinLog监听异常：" + e);
            }
        });
        // 多线程消费
        mysqlBinLogListener.parse();
    }
}
