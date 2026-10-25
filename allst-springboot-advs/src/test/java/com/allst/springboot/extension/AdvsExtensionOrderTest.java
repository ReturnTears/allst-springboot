package com.allst.springboot.extension;

import com.allst.springboot.controller.ExtensionTraceController;
import com.allst.springboot.extension.imports.EnableAdvsExtension;
import com.allst.springboot.extension.model.StartupRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 不连接 MySQL、Redis、Elasticsearch，只核对扩展点在一次真实 refresh 中的先后。
 */
class AdvsExtensionOrderTest {

    @SpringBootApplication
    @EnableAdvsExtension
    @Import(ExtensionTraceController.class)
    static class ExtensionOrderApplication {
    }

    @Test
    void extensionPointsFollowStartupOrder() {
        SpringApplication application = new SpringApplication(ExtensionOrderApplication.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        ConfigurableApplicationContext context = application.run(
                "--spring.profiles.active=extension-it",
                "--server.port=0",
                "--spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration,"
                        + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration");
        try {
            List<StartupRecord> records = StartupTraceLog.snapshot();
            assertBefore(records, StartupPhase.RUN_LISTENER_STARTING, StartupPhase.EVENT_STARTING);
            assertBefore(records, StartupPhase.EVENT_ENVIRONMENT_PREPARED, StartupPhase.ENVIRONMENT_POST_PROCESSOR);
            assertBefore(records, StartupPhase.CONTEXT_INITIALIZER, StartupPhase.RUN_LISTENER_CONTEXT_PREPARED);
            assertBefore(records, StartupPhase.EVENT_PREPARED, StartupPhase.IMPORT_SELECTOR);
            assertBefore(records, StartupPhase.IMPORT_SELECTOR, StartupPhase.IMPORT_REGISTRAR);
            assertBefore(records, StartupPhase.BEAN_DEFINITION_REGISTRY, StartupPhase.BEAN_FACTORY_POST_PROCESSOR);
            assertBefore(records, StartupPhase.WEB_SERVER_CUSTOMIZER, StartupPhase.SERVLET_CONTEXT_INITIALIZER);
            int beforeInstantiation = sequenceOf(records, StartupPhase.BEFORE_INSTANTIATION, null);
            int demoConstructor = sequenceOf(records, StartupPhase.CONSTRUCTOR, "AdvsLifecycleDemoBean");
            int beanNameAware = sequenceOf(records, StartupPhase.AWARE_BEAN_NAME, null);
            assertTrue(beforeInstantiation > 0 && beforeInstantiation < demoConstructor,
                    "实例化前回调应早于示例 Bean 构造器");
            assertTrue(demoConstructor < beanNameAware, "构造器应早于 BeanNameAware");
            int determineConstructor = sequenceOf(records, StartupPhase.DETERMINE_CONSTRUCTOR, null);
            int sampleConstructor = sequenceOf(records, StartupPhase.CONSTRUCTOR, "AdvsConstructorSample");
            assertTrue(determineConstructor > 0 && determineConstructor < sampleConstructor,
                    "组件扫描 Bean 应先推断构造器再执行构造器");
            assertBefore(records, StartupPhase.AWARE_BEAN_NAME, StartupPhase.AWARE_ENVIRONMENT);
            assertBefore(records, StartupPhase.AWARE_ENVIRONMENT, StartupPhase.AWARE_APPLICATION_CONTEXT);
            assertBefore(records, StartupPhase.BEAN_POST_BEFORE_INIT, StartupPhase.POST_CONSTRUCT);
            assertBefore(records, StartupPhase.POST_CONSTRUCT, StartupPhase.INITIALIZING_BEAN);
            assertBefore(records, StartupPhase.INITIALIZING_BEAN, StartupPhase.CUSTOM_INIT_METHOD);
            assertBefore(records, StartupPhase.CUSTOM_INIT_METHOD, StartupPhase.BEAN_POST_AFTER_INIT);
            assertBefore(records, StartupPhase.SMART_INITIALIZING_SINGLETON, StartupPhase.LIFECYCLE_START);
            assertBefore(records, StartupPhase.LIFECYCLE_START, StartupPhase.EVENT_CONTEXT_REFRESHED);
            assertBefore(records, StartupPhase.EVENT_STARTED, StartupPhase.LIVENESS_CHANGED);
            assertBefore(records, StartupPhase.LIVENESS_CHANGED, StartupPhase.APPLICATION_RUNNER);
            assertBefore(records, StartupPhase.APPLICATION_RUNNER, StartupPhase.COMMAND_LINE_RUNNER);
            assertBefore(records, StartupPhase.COMMAND_LINE_RUNNER, StartupPhase.EVENT_READY);
            assertBefore(records, StartupPhase.EVENT_READY, StartupPhase.READINESS_CHANGED);
            assertBefore(records, StartupPhase.FACTORY_BEAN, StartupPhase.SMART_INITIALIZING_SINGLETON);
            Integer port = context.getEnvironment().getProperty("local.server.port", Integer.class);
            ResponseEntity<String> catalog = new RestTemplate().getForEntity(
                    "http://127.0.0.1:" + port + "/advs/extension/catalog", String.class);
            assertEquals(200, catalog.getStatusCodeValue());
            assertEquals("ready", catalog.getHeaders().getFirst("X-Advs-Extension"));
            assertTrue(catalog.getBody() != null && catalog.getBody().contains("EnvironmentPostProcessor"));
            assertTrue(context.containsBean("advsStartupClock"));
            assertTrue(context.containsBean("advsFeatureSwitch"));
            assertTrue(context.containsBean("advsDynamicNotice"));
            assertTrue(context.containsBean("advsImportedMarker"));
        } finally {
            context.close();
        }
        List<StartupRecord> closed = StartupTraceLog.snapshot();
        assertBefore(closed, StartupPhase.EVENT_CONTEXT_CLOSED, StartupPhase.LIFECYCLE_STOP);
        assertBefore(closed, StartupPhase.BEFORE_DESTRUCTION, StartupPhase.PRE_DESTROY);
        assertBefore(closed, StartupPhase.PRE_DESTROY, StartupPhase.DISPOSABLE_BEAN);
        assertBefore(closed, StartupPhase.DISPOSABLE_BEAN, StartupPhase.CUSTOM_DESTROY_METHOD);
    }

    private void assertBefore(List<StartupRecord> records, StartupPhase earlier, StartupPhase later) {
        assertBefore(records, earlier, later, null);
    }

    private void assertBefore(List<StartupRecord> records, StartupPhase earlier, StartupPhase later, String actionPart) {
        int earlierSequence = sequenceOf(records, earlier, actionPart);
        int laterSequence = sequenceOf(records, later, actionPart);
        assertTrue(earlierSequence > 0, "缺少阶段 " + earlier.getTitle());
        assertTrue(laterSequence > 0, "缺少阶段 " + later.getTitle());
        assertTrue(earlierSequence < laterSequence,
                earlier.getTitle() + "#" + earlierSequence + " 应该早于 " + later.getTitle() + "#" + laterSequence);
    }

    private int sequenceOf(List<StartupRecord> records, StartupPhase phase, String actionPart) {
        for (StartupRecord record : records) {
            if (record.getPhaseOrder() != phase.getOrder()) {
                continue;
            }
            if (actionPart == null || record.getAction().contains(actionPart)) {
                return record.getSequence();
            }
        }
        return -1;
    }
}
