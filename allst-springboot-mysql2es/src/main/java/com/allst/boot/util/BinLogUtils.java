package com.allst.boot.util;

import com.allst.boot.mapper.SearchStoreLogoExtMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author Hutu
 * @since 2024-08-09 下午 10:46
 */
@Slf4j
@Component
public class BinLogUtils {
    private static BinLogUtils binLogUtils;

    @Resource
    private SearchStoreLogoExtMapper searchStoreLogoExtMapper;

    @PostConstruct
    public void init() {
        binLogUtils = this;
        binLogUtils.searchStoreLogoExtMapper = this.searchStoreLogoExtMapper;
    }
}
