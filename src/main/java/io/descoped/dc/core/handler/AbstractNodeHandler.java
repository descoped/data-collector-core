package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.node.Node;

public abstract class AbstractNodeHandler<T extends Node> extends AbstractHandler<T> {

    public AbstractNodeHandler(T node) {
        super(node);
    }

    /**
     * Returns an enriched context with global configuration applied (if present)
     *
     * @param context input context
     * @return input context
     */
    @Override
    public ExecutionContext execute(ExecutionContext context) {
        return configureContext(context);
    }
}
