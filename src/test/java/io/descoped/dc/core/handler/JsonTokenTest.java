package io.descoped.dc.core.handler;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.JsonToken;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.test.client.TestClient;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static io.descoped.dc.api.Builders.addContent;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.jqpath;
import static io.descoped.dc.api.Builders.jsonToken;
import static io.descoped.dc.api.Builders.parallel;
import static io.descoped.dc.api.Builders.publish;
import static io.descoped.dc.api.Builders.sequence;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(TestServerExtension.class)
public class JsonTokenTest {

    static final Logger LOG = LoggerFactory.getLogger(JsonTokenTest.class);

    static final String JSON_ARRAY = "[{\"id\":\"1\",\"event-id\":\"1001\"},{\"id\":\"2\",\"event-id\":\"1002\"},{\"id\":\"3\",\"event-id\":\"1003\"},{\"id\":\"4\",\"event-id\":\"1004\"},{\"id\":\"5\",\"event-id\":\"1005\"},{\"id\":\"6\",\"event-id\":\"1006\"},{\"id\":\"7\",\"event-id\":\"1007\"},{\"id\":\"8\",\"event-id\":\"1008\"},{\"id\":\"9\",\"event-id\":\"1009\"},{\"id\":\"10\",\"event-id\":\"1010\"}]";

    @Inject
    TestServer server;

    @Inject
    TestClient client;

    @Test
    public void jsonTokenDocumentParser() {
        DocumentParserFeature parser = Queries.parserFor(JsonToken.class);
        InputStream source = new ByteArrayInputStream(JSON_ARRAY.getBytes());
        parser.tokenDeserializer(source, entry -> LOG.trace("{}", new String(parser.serialize(entry))));
    }

    @Test
    public void getJsonArray() throws IOException {
        Request request = Request.newRequestBuilder()
                .url(server.testURL("/api/events?gzip=true&position=1&pageSize=10"))
                .GET()
                .build();

        Response response = Client.newClient()
                .sendAsync(request, TempFileBodyHandler.ofFile())
                .thenApply(this::validate)
                .join();

        TempFileBodyHandler fileBodyHandler = (TempFileBodyHandler) response.<Path>bodyHandler().orElseThrow();
        LOG.trace("Response [{}]:", response.statusCode());
        try (BufferedReader reader = Files.newBufferedReader(fileBodyHandler.body())) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.printf("%s%n", line);
            }
        } finally {
            fileBodyHandler.dispose();
        }
    }

    private Response validate(Response response) {
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Expected 200 OK");
        }
        return response;
    }

    @Test
    public void jsonTokenSequentialParser() {
        ExecutionContext output = Worker.newBuilder()
                .specification(Specification.start("test", "getPage", "page")
                        .function(get("page")
                                .url(server.testURL("/api/events?gzip=true&position=${fromPosition}&pageSize=10"))
                                .pipe(sequence(jsonToken())
                                        .expected(jqpath(".id"))
                                )
                                //.pipe(console())
                                .pipe(parallel(jsonToken())
                                        .variable("position", jqpath(".id"))
                                        .pipe(addContent("${position}", "entry"))
                                        .pipe(publish("${position}"))
                                )
                        )
                )
                .configuration(Map.of("content.stream.connector", "discarding"))
                .header("Accept", "application/json")
                .variable("fromPosition", 1)
                .build()
                .run();

        assertNotNull(output);
    }

    @Disabled
    @Test
    public void brregJsonTokenSequentialParser() {
        ExecutionContext output = Worker.newBuilder()
                .specification(Specification.start("test", "getPage", "page")
                        .function(get("page")
                                .url("https://data.brreg.no/enhetsregisteret/api/enheter/lastned")
                                .pipe(sequence(jsonToken())
                                        .expected(jqpath(".organisasjonsnummer"))
                                )
                                .pipe(parallel(jsonToken())
                                        .variable("position", jqpath(".organisasjonsnummer"))
                                        .pipe(addContent("${position}", "entry"))
                                        .pipe(publish("${position}"))
                                )
                        )
                )
                .configuration(Map.of(
                                "content.stream.connector", "discarding",
                                "data.collector.http.client.timeout.seconds", "3600",
                                "data.collector.http.request.timeout.seconds", "3600"
                        )
                )
                .build()
                .run();

        assertNotNull(output);
    }
}
