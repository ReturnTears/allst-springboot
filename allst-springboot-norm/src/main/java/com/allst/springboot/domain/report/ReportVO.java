package com.allst.springboot.domain.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportVO {

    private long userCount;
    private long orderCount;
    private LocalDateTime generatedAt;
}
