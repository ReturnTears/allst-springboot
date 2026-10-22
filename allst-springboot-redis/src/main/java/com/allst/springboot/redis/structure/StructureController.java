package com.allst.springboot.redis.structure;

import com.allst.springboot.redis.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据结构示例入口。demo 会重放一组命令；play 用来在控制台里改自己的键。
 */
@RestController
@RequestMapping("/api/structure")
public class StructureController {

    private final StructureService structureService;

    public StructureController(StructureService structureService) {
        this.structureService = structureService;
    }

    @GetMapping("/{type}/demo")
    public ApiResult<?> demo(@PathVariable String type) {
        return ApiResult.ok(structureService.demo(type));
    }

    @PostMapping("/{type}/play")
    public ApiResult<?> play(@PathVariable String type, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(structureService.play(type, body));
    }
}
