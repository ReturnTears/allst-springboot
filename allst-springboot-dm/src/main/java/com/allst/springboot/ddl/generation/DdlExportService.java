package com.allst.springboot.ddl.generation;

import com.allst.springboot.common.BizException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件导出。按方言一个脚本文件，UTF-8 写入配置的输出目录。
 * resolve() 做路径防穿越校验，下载接口只能拿到 output-dir 下的文件。
 */
@Service
public class DdlExportService {

    private final Path baseDir;

    public DdlExportService(@Value("${dm.ddl.output-dir:./generated-ddl}") String outputDir) {
        this.baseDir = Paths.get(outputDir).toAbsolutePath().normalize();
    }

    public List<ExportedFile> export(DdlPlan plan) {
        try {
            Files.createDirectories(baseDir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            List<ExportedFile> result = new ArrayList<>();
            for (DialectScript script : plan.getScripts()) {
                String name = "ddl-" + safe(script.getDialect()) + "-" + stamp + ".sql";
                Path file = resolve(name);
                Files.write(file, script.getSql().getBytes(StandardCharsets.UTF_8));
                result.add(new ExportedFile(name, script.getDialect(), Files.size(file)));
            }
            return result;
        } catch (IOException e) {
            throw new BizException(500, "导出 DDL 文件失败: " + e.getMessage());
        }
    }

    /** 解析下载路径，拒绝越界与子目录 */
    public Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BizException(400, "文件名不能为空");
        }
        Path target = baseDir.resolve(fileName).normalize();
        if (!target.startsWith(baseDir) || !target.getFileName().toString().equals(fileName)) {
            throw new BizException(400, "非法文件名: " + fileName);
        }
        return target;
    }

    public List<String> listFiles() {
        if (!Files.exists(baseDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> paths = Files.list(baseDir)) {
            return paths.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public String outputDir() {
        return baseDir.toString();
    }

    private static String safe(String s) {
        return s.replaceAll("[^A-Za-z0-9_-]", "_").toUpperCase(Locale.ROOT);
    }

    @Getter
    public static final class ExportedFile {
        private final String fileName;
        private final String dialect;
        private final long size;

        public ExportedFile(String fileName, String dialect, long size) {
            this.fileName = fileName;
            this.dialect = dialect;
            this.size = size;
        }
    }
}
