package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.BodyContains;
import io.descoped.dc.api.node.ResponsePredicate;

@Handler(forClass = BodyContains.class)
public class BodyContainsHandler extends AbstractResponsePredicateHandler<BodyContains> {

    public BodyContainsHandler(BodyContains node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        Response response = context.state(Response.class);
        if (response == null) {
            return ExecutionContext.empty();
        }

        String queryResult = Queries.from(context, node.getQuery()).evaluateStringLiteral(response.body());

        Boolean test = node.getEqualToStringLiteral().equals(queryResult);

        return ExecutionContext.empty().state(ResponsePredicate.RESPONSE_PREDICATE_RESULT, test);
    }

}
