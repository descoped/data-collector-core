package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryResult;
import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.XmlToken;

import java.nio.file.Path;

@Handler(forClass = XmlToken.class)
public class XmlTokenHandler extends AbstractHandler<XmlToken> {

    private final DocumentParserFeature jsonParser;

    public XmlTokenHandler(XmlToken node) {
        super(node);
        jsonParser = Queries.parserFor(node.getClass());
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        Response response = context.state(Response.class);

        // todo is this used anywhere? maybe an empty EC should be returned
        BodyHandler<Path> bodyHandler = response.<Path>bodyHandler().orElseThrow();
        QueryResult<Path> queryResult = new QueryResult<>(bodyHandler.body());
        ExecutionContext output = ExecutionContext.empty().state(QueryResult.class, queryResult);
        return output;
    }


}
