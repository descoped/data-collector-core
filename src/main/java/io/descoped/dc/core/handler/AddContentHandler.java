package io.descoped.dc.core.handler;

import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.AddContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@Handler(forClass = AddContent.class)
public class AddContentHandler extends AbstractNodeHandler<AddContent> {

    private static final Logger LOG = LoggerFactory.getLogger(AddContentHandler.class);

    public AddContentHandler(AddContent node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        ExpressionLanguage el = new ExpressionLanguage(context);
        String position = (String) el.evaluateExpression(node.positionVariableExpression());

        PageEntryState pageEntryState = context.state(PageEntryState.class);

        ContentStore contentStore = context.services().get(ContentStore.class);

        boolean bufferResponseBody = context.state(ParallelHandler.ADD_BODY_CONTENT) == null ? false : context.state(ParallelHandler.ADD_BODY_CONTENT);

        // evaluate state - single expression for key and multiple expressions for value
        HttpRequestInfo httpRequestInfo = context.state(HttpRequestInfo.class);
        // Limitation: only save state for response-type: entry is supported
        Map<String, Object> evaluatedState = new LinkedHashMap<>();
        if (!bufferResponseBody && !node.state().isEmpty()) {
            for (Map.Entry<String, Object> entry : node.state().entrySet()) {
                String key = entry.getKey();

                if (el.isExpression(key)) {
                    key = (String) el.evaluateExpression(key);
                }

                Object value = entry.getValue();

                if (value instanceof String && el.isExpression((String) value)) {
                    value = el.evaluateExpressions((String) value);
                }

                evaluatedState.put(key, value);
            }
        }

        String topicName = node.configurations().flowContext().topic();
        if (topicName == null) {
            throw new IllegalStateException("Unable to resolve topic!");
        }

        String contentKey = node.contentKey();
        if (el.isExpression(contentKey)) {
            contentKey = el.evaluateExpressions(contentKey);
        }

        byte[] content = bufferResponseBody ? context.state(Response.class).body() : pageEntryState.content;
        //LOG.trace("Content ({}):\n{}", position, new String(content));

        // feature request: decompression of 'content' can be handled here.

        if (bufferResponseBody) {
            contentStore.bufferDocument(topicName, position, contentKey, content, httpRequestInfo);
        } else {
            contentStore.bufferPaginationEntryDocument(topicName, position, contentKey, content, httpRequestInfo, evaluatedState);
        }

        return ExecutionContext.empty();
    }
}
