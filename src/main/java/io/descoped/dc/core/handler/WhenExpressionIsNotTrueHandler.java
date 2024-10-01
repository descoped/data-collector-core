package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.node.WhenExpressionIsTrue;
import io.descoped.dc.core.handler.state.ConditionType;

@Handler(forClass = WhenExpressionIsTrue.class)
public class WhenExpressionIsNotTrueHandler extends AbstractHandler<WhenExpressionIsTrue> {

    private final WhenExpressionIsTrue node;

    public WhenExpressionIsNotTrueHandler(WhenExpressionIsTrue node) {
        super(node);
        this.node = node;
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        ExecutionContext output = ExecutionContext.empty();

        final String expression = node.identifier();

        ExpressionLanguage el = new ExpressionLanguage(input);
        if (!el.isExpression(expression)) {
            throw new IllegalStateException("Not a valid expression: " + expression);
        }
        boolean isTrue = (boolean) el.evaluateExpression(expression);

        output.state(ConditionType.UNTIL_CONDITION_RESULT, isTrue);
        return output;
    }
}
