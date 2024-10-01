package io.descoped.dc.core.executor;

import io.descoped.dc.api.Execution;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.core.handler.Handlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    public static <N extends Base> Execution instanceOf(N node) {
        return Handlers.createHandlerFor(node);
    }

    public static <N extends Base> ExecutionContext execute(N node, ExecutionContext input) {
        Execution executionHandler = Handlers.createHandlerFor(node);
        return executionHandler.execute(input);
    }

}
