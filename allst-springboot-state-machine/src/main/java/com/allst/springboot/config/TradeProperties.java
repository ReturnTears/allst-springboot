package com.allst.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "statemachine.trade")
public class TradeProperties {

    /** 大于等于该分数时，支付成功后进入人工风控。 */
    private int highRiskScore = 80;

    /** 待支付、支付中、支付失败超过该分钟数后由扫描器关闭。 */
    private int payTimeoutMinutes = 30;

    private long scanDelayMs = 60000L;

    public int getHighRiskScore() {
        return highRiskScore;
    }

    public void setHighRiskScore(int highRiskScore) {
        this.highRiskScore = highRiskScore;
    }

    public int getPayTimeoutMinutes() {
        return payTimeoutMinutes;
    }

    public void setPayTimeoutMinutes(int payTimeoutMinutes) {
        this.payTimeoutMinutes = payTimeoutMinutes;
    }

    public long getScanDelayMs() {
        return scanDelayMs;
    }

    public void setScanDelayMs(long scanDelayMs) {
        this.scanDelayMs = scanDelayMs;
    }
}
