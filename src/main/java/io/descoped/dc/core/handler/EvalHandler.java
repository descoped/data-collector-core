package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryException;
import io.descoped.dc.api.handler.QueryState;
import io.descoped.dc.api.node.Eval;

import java.util.List;

@Handler(forClass = Eval.class)
public class EvalHandler extends AbstractQueryHandler<Eval> {

    public EvalHandler(Eval node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        QueryState<Eval> queryState = input.state(QueryState.class);

        if (queryState == null) {
            throw new IllegalArgumentException("QueryState is not set!");
        }

        if (queryState.type() != Type.STRING_LITERAL) {
            throw new QueryException("Only QueryFeature.Type.STRING_LITERAL is supported!");
        }

        /*
         * execute sub-query and bind variable to output
         */

        if (node.query() != null) {
            String result = Queries.from(input, node.query()).evaluateStringLiteral(queryState.data());
            input.variable(node.bind(), result);
        }

        /*
         * execute this handler
         */

        ExecutionContext evalContext = ExecutionContext.of(input);
        ExpressionLanguage el = new ExpressionLanguage(input);
        evalContext.state(QueryState.class, new QueryState<>(queryState.type(), el));

//        return output.merge(super.execute(evalContext));
        return super.execute(evalContext);
    }

    @Override
    public List<?> evaluateList(Object data) {
        throw new UnsupportedOperationException("queryList is not supported!");
    }

    @Override
    public Object evaluateObject(Object data) {
        throw new UnsupportedOperationException("queryObject is not supported!");
    }

    @Override
    public String evaluateStringLiteral(Object data) {
        ExpressionLanguage el = (ExpressionLanguage) data;
        Object value = el.evaluateExpression(node.expression());
        return (value != null ? value.toString() : null);
    }
}
