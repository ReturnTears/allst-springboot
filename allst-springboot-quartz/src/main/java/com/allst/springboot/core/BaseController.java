package com.allst.springboot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hutu
 * @since 2023-05-30 下午 09:38
 */
public class BaseController {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * api 返回使用，状态
     */
    protected int success = 0;
    protected int error = 1;
}
