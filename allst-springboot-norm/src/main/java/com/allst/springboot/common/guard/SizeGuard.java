package com.allst.springboot.common.guard;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.NormProperties;
import org.springframework.stereotype.Component;

/**
 * 服务层的条数防线。控制器上的校验拦外部请求，这里防止其它入口绕过注解。
 * 对应规范：10 限制记录条数。
 */
@Component
public class SizeGuard {

    private final NormProperties properties;

    public SizeGuard(NormProperties properties) {
        this.properties = properties;
    }

    public int pageIndex(int page) {
        int maxPage = properties.getPage().getMaxPage();
        if (page < 0 || page > maxPage) {
            throw new BizException(ResultCode.PAGE_LIMIT, "页码需在 0 到 " + maxPage + " 之间");
        }
        return page;
    }

    public int pageSize(int size) {
        int maxSize = properties.getPage().getMaxSize();
        if (size < 1 || size > maxSize) {
            throw new BizException(ResultCode.PAGE_LIMIT, "每页条数需在 1 到 " + maxSize + " 之间");
        }
        return size;
    }

    public void batch(int size) {
        int maxSize = properties.getBatch().getMaxSize();
        if (size < 1 || size > maxSize) {
            throw new BizException(ResultCode.BATCH_LIMIT, "批量条数需在 1 到 " + maxSize + " 之间");
        }
    }
}
