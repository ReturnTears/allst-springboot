package com.allst.boot.domain;

/**
 * 下单渠道。枚举自带业务属性，配合 Stream 分组时不必再写 if-else 判断渠道类型。
 */
public enum Channel {

    APP("移动端", true),
    MINI_PROGRAM("小程序", true),
    WEB("网页", false),
    OFFLINE("线下门店", false);

    private final String label;
    private final boolean mobile;

    Channel(String label, boolean mobile) {
        this.label = label;
        this.mobile = mobile;
    }

    public String getLabel() {
        return label;
    }

    /** 是否移动端。把判断收进枚举，调用方只问结果，不关心有哪些渠道。 */
    public boolean isMobile() {
        return mobile;
    }
}