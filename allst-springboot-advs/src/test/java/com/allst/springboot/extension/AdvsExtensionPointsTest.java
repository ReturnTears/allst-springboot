package com.allst.springboot.extension;

import com.allst.springboot.extension.bootstrap.AdvsEnvironmentPostProcessor;
import com.allst.springboot.extension.bootstrap.AdvsStartupException;
import com.allst.springboot.extension.bootstrap.AdvsStartupFailureAnalyzer;
import com.allst.springboot.extension.catalog.ExtensionCatalog;
import com.allst.springboot.extension.imports.AdvsExtensionImportSelector;
import com.allst.springboot.extension.model.ExtensionPointDoc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvsExtensionPointsTest {

    @Test
    void catalogCoversEveryPhaseInOrder() {
        List<ExtensionPointDoc> points = ExtensionCatalog.create().getPoints();
        Set<Integer> orders = new HashSet<>();
        int previous = Integer.MIN_VALUE;
        for (ExtensionPointDoc point : points) {
            assertTrue(point.getOrder() > previous, point.getPhase());
            assertTrue(orders.add(point.getOrder()), "重复阶段号 " + point.getOrder());
            assertTrue(point.getTiming().length() > 0, point.getPhase());
            assertTrue(point.getCapabilities().length() > 0, point.getPhase());
            assertTrue(point.getProjectScenario().length() > 0, point.getPhase());
            previous = point.getOrder();
        }
        for (StartupPhase phase : StartupPhase.values()) {
            assertTrue(orders.contains(phase.getOrder()), "目录缺少 " + phase.name());
        }
    }

    @Test
    void environmentDefaultsDoNotOverrideExistingValues() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test",
                Collections.singletonMap("advs.extension.banner", "custom-banner")));
        new AdvsEnvironmentPostProcessor().postProcessEnvironment(environment,
                new SpringApplication(AdvsExtensionPointsTest.class));

        assertEquals("custom-banner", environment.getProperty("advs.extension.banner"));
        assertEquals("false", environment.getProperty("advs.extension.cache-warmup"));
        assertEquals("false", environment.getProperty("advs.extension.fail-fast"));
    }

    @Test
    void failFastRequiresToken() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test",
                Collections.singletonMap("advs.extension.fail-fast", "true")));
        AdvsEnvironmentPostProcessor processor = new AdvsEnvironmentPostProcessor();
        SpringApplication application = new SpringApplication(AdvsExtensionPointsTest.class);

        AdvsStartupException exception = assertThrows(AdvsStartupException.class,
                () -> processor.postProcessEnvironment(environment, application));
        assertTrue(exception.getMessage().contains("required-token"));
    }

    @Test
    void failureAnalyzerOnlyExplainsStartupException() {
        FailureAnalysis analysis = new AdvsStartupFailureAnalyzer()
                .analyze(new AdvsStartupException("missing token"));
        assertTrue(analysis != null && analysis.getDescription().contains("missing token"));
        assertTrue(analysis.getAction().contains("fail-fast"));
        assertNull(new AdvsStartupFailureAnalyzer().analyze(new IllegalStateException("other")));
    }

    @Test
    void importSelectorPointsOutsideComponentScan() {
        AnnotationMetadata metadata = AnnotationMetadata.introspect(AdvsExtensionPointsTest.class);
        String[] imports = new AdvsExtensionImportSelector().selectImports(metadata);
        assertEquals(1, imports.length);
        assertEquals("com.allst.advs.autoconfigure.AdvsImportedConfiguration", imports[0]);
    }
}
