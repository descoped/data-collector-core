package io.descoped.dc.core.handler;

import io.descoped.dc.api.PageContext;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.NextPage;
import io.descoped.dc.api.node.Query;

import java.util.Map;

@Handler(forClass = NextPage.class)
public class NextPageHandler extends AbstractNodeHandler<NextPage> {

    public NextPageHandler(NextPage node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);
        ExecutionContext output = ExecutionContext.empty();

        Response response = input.state(Response.class);
        byte[] body = response.body();

        PageContext.Builder pageContextBuilder = input.state(PageContext.Builder.class);

        for (Map.Entry<String, Query> entry : node.outputs().entrySet()) {
            String variableName = entry.getKey();
            Query variableQuery = entry.getValue();
            String variableValue = Queries.from(input, variableQuery).evaluateStringLiteral(body);
            output.variables().put(variableName, variableValue);
            pageContextBuilder.addNextPosition(variableName, variableValue);
        }

        return output;
    }
}
