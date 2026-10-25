package com.allst.springboot.common.async;

import com.allst.springboot.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/async-tasks")
public class AsyncTaskController {

    private final AsyncTaskRecorder recorder;

    public AsyncTaskController(AsyncTaskRecorder recorder) {
        this.recorder = recorder;
    }

    @GetMapping
    public Result<List<String>> list() {
        return Result.ok(recorder.snapshot());
    }
}
