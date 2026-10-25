package com.allst.springboot.controller;

import com.allst.springboot.entity.Result;
import com.allst.springboot.extension.StartupTraceLog;
import com.allst.springboot.extension.bootstrap.AdvsStartupClock;
import com.allst.springboot.extension.catalog.ExtensionCatalog;
import com.allst.springboot.extension.imports.AdvsDynamicNotice;
import com.allst.springboot.extension.imports.AdvsImportedMarker;
import com.allst.springboot.extension.model.CachePointDoc;
import com.allst.springboot.extension.model.ExtensionPointDoc;
import com.allst.springboot.extension.model.ExtensionSummary;
import com.allst.springboot.extension.model.StartupTraceView;
import com.allst.springboot.extension.processor.AdvsFeatureSwitch;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动扩展点的回溯接口。上下文路径来自 server.servlet.context-path，当前是 /advs。
 */
@RestController
@RequestMapping("/extension")
public class ExtensionTraceController {

    private final ExtensionCatalog catalog;
    private final AdvsFeatureSwitch featureSwitch;
    private final AdvsStartupClock startupClock;
    private final AdvsDynamicNotice dynamicNotice;
    private final AdvsImportedMarker importedMarker;
    private final Environment environment;

    public ExtensionTraceController(ExtensionCatalog catalog,
                                    AdvsFeatureSwitch featureSwitch,
                                    AdvsStartupClock startupClock,
                                    AdvsDynamicNotice dynamicNotice,
                                    AdvsImportedMarker importedMarker,
                                    Environment environment) {
        this.catalog = catalog;
        this.featureSwitch = featureSwitch;
        this.startupClock = startupClock;
        this.dynamicNotice = dynamicNotice;
        this.importedMarker = importedMarker;
        this.environment = environment;
    }

    @GetMapping({"", "/"})
    public Result<ExtensionSummary> summary() {
        List<StartupTraceView> views = catalog.explain(StartupTraceLog.snapshot());
        List<String> latest = new ArrayList<>();
        int from = Math.max(0, views.size() - 8);
        for (int i = from; i < views.size(); i++) {
            StartupTraceView view = views.get(i);
            latest.add(view.getSequence() + ". [phase=" + view.getPhaseOrder() + "] "
                    + view.getPhase() + " | " + view.getAction());
        }
        String prefix = environment.getProperty("server.servlet.context-path", "");
        ExtensionSummary summary = new ExtensionSummary(
                catalog.getSpringBootVersion(),
                featureSwitch.getBanner(),
                featureSwitch.isCacheWarmup(),
                dynamicNotice.getMessage(),
                importedMarker.getMessage(),
                startupClock.getEpochMillis(),
                startupClock.elapsedMillis(),
                views.size(),
                catalog.getPoints().size(),
                StartupTraceLog.cachePoints().size(),
                List.of(
                        prefix + "/extension",
                        prefix + "/extension/catalog",
                        prefix + "/extension/trace",
                        prefix + "/extension/cache-points"),
                latest);
        return new Result<>(200, "ok", summary);
    }

    @GetMapping("/catalog")
    public Result<List<ExtensionPointDoc>> catalog() {
        return new Result<>(200, "ok", catalog.getPoints());
    }

    @GetMapping("/trace")
    public Result<List<StartupTraceView>> trace() {
        return new Result<>(200, "ok", catalog.explain(StartupTraceLog.snapshot()));
    }

    @GetMapping("/cache-points")
    public Result<List<CachePointDoc>> cachePoints() {
        return new Result<>(200, "ok", StartupTraceLog.cachePoints());
    }
}
