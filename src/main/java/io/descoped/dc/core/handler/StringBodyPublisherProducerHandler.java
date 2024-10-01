package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.node.BodyPublisherProducer;
import io.descoped.dc.api.node.StringBodyPublisherProducer;

@Handler(forClass = StringBodyPublisherProducer.class)
public class StringBodyPublisherProducerHandler extends AbstractHandler<StringBodyPublisherProducer> {

    public StringBodyPublisherProducerHandler(StringBodyPublisherProducer node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        String text = evaluateExpression(context, node.text());
        return ExecutionContext.empty().state(BodyPublisherProducer.class, text.getBytes());
    }

    private String evaluateExpression(ExecutionContext context, String text) {
        ExpressionLanguage el = new ExpressionLanguage(context);
        if (el.isExpression(text)) {
            return el.evaluateExpressions(text);
        } else {
            return text;
        }
    }

}
