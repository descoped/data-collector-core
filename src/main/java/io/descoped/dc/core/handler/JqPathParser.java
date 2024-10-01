package io.descoped.dc.core.handler;

import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.SupportHandler;
import io.descoped.dc.api.node.JqPath;
import io.descoped.dc.api.util.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@SupportHandler(forClass = JqPath.class, selectorClass = DocumentParserFeature.class)
public class JqPathParser implements DocumentParserFeature {

    private final JsonParser jsonParser;

    public JqPathParser() {
        jsonParser = JsonParser.createJsonParser();
    }

    /**
     * @param document ObjectNode (JsonNode or ArrayNode)
     * @return byte array
     */
    @Override
    public byte[] serialize(Object document) {
        return jsonParser.toJSON(document).getBytes();
    }

    /**
     * @param source json byte array
     * @return JsonNode
     */
    @Override
    public Object deserialize(byte[] source) {
        try {
            return jsonParser.mapper().readTree(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void tokenDeserializer(InputStream source, Consumer<Object> entryCallback) {
        throw new UnsupportedOperationException();
    }
}
