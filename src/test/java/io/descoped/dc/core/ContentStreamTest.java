package io.descoped.dc.core;

import io.descoped.dc.api.content.ContentStream;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamProducer;
import io.descoped.dc.content.provider.rawdata.RawdataClientContentStream;
import io.descoped.rawdata.api.RawdataClient;
import io.descoped.rawdata.api.RawdataClientInitializer;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class ContentStreamTest {

    @Test
    public void thatRawdataClient() {
        RawdataClient client = ProviderConfigurator.configure(Map.of(), "memory", RawdataClientInitializer.class);
        ContentStream contentStream = new RawdataClientContentStream(client);
        ContentStreamProducer producer = contentStream.producer("ns");

        ContentStreamBuffer.Builder builder = producer.builder();
        builder.position("1")
                .buffer("a", new byte[7], null)
                .buffer("b", new byte[5], null);

        producer.produce(builder);

        producer.publish("1");
    }
}
