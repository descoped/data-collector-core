package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryResult;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.Body;

import java.nio.charset.StandardCharsets;

@Handler(forClass = Body.class)
public class BodyHandler extends AbstractHandler<Body> {

    public BodyHandler(Body node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        Response response = context.state(Response.class);
        String body = new String(response.body(), StandardCharsets.UTF_8);
        QueryResult<String> queryResult = new QueryResult<>(body);
        ExecutionContext output = ExecutionContext.empty().state(QueryResult.class, queryResult);
        return output;
    }
}
