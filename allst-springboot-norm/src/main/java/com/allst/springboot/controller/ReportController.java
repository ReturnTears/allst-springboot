package com.allst.springboot.controller;

import com.allst.springboot.annotation.RequireSign;
import com.allst.springboot.common.api.Result;
import com.allst.springboot.domain.report.ReportService;
import com.allst.springboot.domain.report.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequireSign
@RequestMapping("/api/v1/reports")
@Tag(name = "异步报表")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "异步汇总用户数和订单数", description = "统计在业务线程池执行，HTTP 线程不占用到查询结束。")
    public CompletableFuture<Result<ReportVO>> summary() {
        return reportService.summary().thenApply(Result::ok);
    }
}
