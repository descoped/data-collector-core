package io.descoped.dc.core.http;

import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.core.metrics.MetricsAgent;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;

public class HttpClientExporterTest {

    @BeforeAll
    public static void beforeAll() {
        MetricsAgent.premain(null, ByteBuddyAgent.install());
    }

    @Test
    public void thatByteBuddyAgentInterceptsSend() throws Exception {
        Client client = Client.newClient();
        Response resp = client.send(Request.newRequestBuilder().GET().url("https://www.google.com/").build());
        StringWriter sw = new StringWriter();
        TextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        System.out.printf("%s%n", sw);
    }

    @Test
    public void thatByteBuddyAgentInterceptsSendAsync() throws Exception {
        Client client = Client.newClient();
        CompletableFuture<Response> resp = client.sendAsync(Request.newRequestBuilder().GET().url("https://www.google.com/").build());
        resp.get();
        StringWriter sw = new StringWriter();
        TextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        System.out.printf("%s%n", sw);
    }
}
