package io.descoped.dc.core.handler;

import io.descoped.dc.api.node.Base;

public abstract class AbstractResponsePredicateHandler<N extends Base> extends AbstractHandler<N> {

    public AbstractResponsePredicateHandler(N node) {
        super(node);
    }

}
