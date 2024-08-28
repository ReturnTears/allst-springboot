package com.allst.springboot.service.impl;

import com.allst.springboot.anno.OnChange;
import com.allst.springboot.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Hutu
 * @since 2024-08-28 下午 09:57
 */
@Service
public class DemoServiceImpl implements DemoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoServiceImpl.class);
    @Override
    @OnChange
    public String get(String name, Object... obj) {
        LOGGER.info("come in method DemoServiceImpl.get");
        return "DemoServiceImpl.get : " + name;
    }
}
