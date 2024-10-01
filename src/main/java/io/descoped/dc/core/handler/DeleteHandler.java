package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.Delete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
@Handler(forClass = Delete.class)
public class DeleteHandler extends OperationHandler<Delete> {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteHandler.class);

    public DeleteHandler(Delete node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);
        int requestTimeout = beforeRequest(input);

        Request.Builder requestBuilder = Request.newRequestBuilder().DELETE(); // . timeout(Duration.ofSeconds(requestTimeout));

        Response response = doRequest(input, requestTimeout, requestBuilder);

        return handleResponse(input, response);
    }

}