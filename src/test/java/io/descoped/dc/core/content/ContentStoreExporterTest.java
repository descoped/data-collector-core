package io.descoped.dc.core.content;

import io.descoped.dc.api.CorrelationIds;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Headers;
import io.descoped.service.provider.api.ProviderConfigurator;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ContentStoreExporterTest {

    @BeforeAll
    public static void beforeAll() {
        ContentStoreAgent.install(ByteBuddyAgent.install());
    }

    @AfterAll
    public static void afterAll() throws IOException {
        StringWriter sw = new StringWriter();
        TextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        System.out.printf("%s%n", sw);
    }

    @Test
    public void name() {
        ContentStore contentStore = ProviderConfigurator.configure(
                Map.of("rawdata.client.provider", "memory"),
                "rawdata",
                ContentStoreInitializer.class
        );
        assertNotNull(contentStore);
        HttpRequestInfo httpRequestInfo = new HttpRequestInfo(CorrelationIds.create(ExecutionContext.empty()), null, -1, new Headers(), new Headers(), -1);
        contentStore.addPaginationDocument("topic", "1", "page", "PAGINATION_PAGE".getBytes(), httpRequestInfo);
        contentStore.bufferPaginationEntryDocument("topic", "1", "page", "PAGINATION_PAGE_ENTRY".getBytes(), httpRequestInfo, new LinkedHashMap<>());
        contentStore.bufferDocument("topic", "1", "entry", "PAYLOAD".getBytes(), httpRequestInfo);
        assertEquals(2, contentStore.contentKeys("topic", "1").size());
        contentStore.publish("topic", "1");
    }

}
