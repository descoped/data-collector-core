package io.descoped.dc.core.metrics;

import io.descoped.dc.core.content.ContentStoreAgent;
import io.descoped.dc.core.http.HttpClientAgent;

import java.lang.instrument.Instrumentation;

public class MetricsAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        HttpClientAgent.install(inst);
        ContentStoreAgent.install(inst);
    }

}
