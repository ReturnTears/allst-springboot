package com.allst.springboot.ddl.api;

import com.allst.springboot.common.BizException;
import com.allst.springboot.common.Result;
import com.allst.springboot.ddl.dialect.DialectRouter;
import com.allst.springboot.ddl.generation.DdlExportService;
import com.allst.springboot.ddl.generation.DdlGenerator;
import com.allst.springboot.ddl.generation.DdlPlan;
import com.allst.springboot.ddl.generation.DdlRequest;
import com.allst.springboot.ddl.meta.MetadataSeeder;
import com.allst.springboot.ddl.meta.TableMeta;
import com.allst.springboot.ddl.meta.TableMetaRepository;
import lombok.Getter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据驱动 DDL 生成的演示接口。
 * generate/export 既接受请求体里自带的元数据（离线生成），也能不传 body 直接读库里种子数据。
 */
@RestController
@RequestMapping("/api/dm/ddl")
public class DdlController {

    private final DialectRouter router;
    private final DdlGenerator generator;
    private final DdlExportService exporter;
    private final TableMetaRepository repository;
    private final MetadataSeeder seeder;

    public DdlController(DialectRouter router, DdlGenerator generator, DdlExportService exporter,
                         TableMetaRepository repository, MetadataSeeder seeder) {
        this.router = router;
        this.generator = generator;
        this.exporter = exporter;
        this.repository = repository;
        this.seeder = seeder;
    }

    /** 方言能力矩阵：每种方言支持哪些能力、哪些逻辑类型 */
    @GetMapping("/dialects")
    public Result<List<DialectInfo>> dialects() {
        List<DialectInfo> infos = router.all().stream()
                .map(d -> new DialectInfo(
                        d.name(),
                        d.capabilities().stream().map(Enum::name).sorted().collect(Collectors.toList()),
                        d.supportedTypes().stream().sorted().collect(Collectors.toList()),
                        "GENERIC".equals(d.name())))
                .collect(Collectors.toList());
        return Result.ok(infos);
    }

    /** 预览生成结果，不落盘 */
    @PostMapping("/generate")
    public Result<DdlPlan> generate(@RequestBody(required = false) DdlRequest request) {
        return Result.ok(generator.generate(dialectOf(request), tablesOf(request)));
    }

    /** 导出 SQL 文件（按方言一个文件） */
    @PostMapping("/export")
    public Result<Map<String, Object>> export(@RequestBody(required = false) DdlRequest request) {
        DdlPlan plan = generator.generate(dialectOf(request), tablesOf(request));
        if (plan.getScripts().isEmpty()) {
            throw new BizException(400, "没有可导出的元数据，请先调用 /seed");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("outputDir", exporter.outputDir());
        data.put("files", exporter.export(plan));
        return Result.ok(data);
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        Path file = exporter.resolve(fileName);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new BizException(404, "文件不存在: " + fileName);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(new FileSystemResource(file));
    }

    @GetMapping("/files")
    public Result<List<String>> files() {
        return Result.ok(exporter.listFiles());
    }

    /** 灌入多方言示例元数据 */
    @PostMapping("/seed")
    public Result<Integer> seed() {
        return Result.ok(seeder.seed());
    }

    private String dialectOf(DdlRequest request) {
        return request == null ? null : request.getDialect();
    }

    private List<TableMeta> tablesOf(DdlRequest request) {
        if (request != null && request.getTables() != null && !request.getTables().isEmpty()) {
            return request.getTables();
        }
        String dialect = dialectOf(request);
        if (dialect != null && !dialect.isBlank()) {
            return repository.findByDialectOrderByIdAsc(dialect.trim().toUpperCase(Locale.ROOT));
        }
        return repository.findAll();
    }

    @Getter
    public static final class DialectInfo {
        private final String name;
        private final List<String> capabilities;
        private final List<String> supportedTypes;
        private final boolean fallback;

        DialectInfo(String name, List<String> capabilities, List<String> supportedTypes, boolean fallback) {
            this.name = name;
            this.capabilities = new ArrayList<>(capabilities);
            this.supportedTypes = new ArrayList<>(supportedTypes);
            this.fallback = fallback;
        }
    }
}
