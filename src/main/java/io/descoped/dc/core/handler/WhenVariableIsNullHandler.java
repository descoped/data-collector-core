package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.node.WhenVariableIsNull;
import io.descoped.dc.core.handler.state.ConditionType;

@Handler(forClass = WhenVariableIsNull.class)
public class WhenVariableIsNullHandler extends AbstractHandler<WhenVariableIsNull> {

    private final WhenVariableIsNull node;

    public WhenVariableIsNullHandler(WhenVariableIsNull node) {
        super(node);
        this.node = node;
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        ExecutionContext output = ExecutionContext.empty();

        final String variableName = node.identifier();
        final boolean variableExists = input.variables().containsKey(variableName);
        final Object variableValue = input.variables().get(variableName);

        boolean isNull = !variableExists || variableValue == null;
        output.state(ConditionType.UNTIL_CONDITION_RESULT, isNull);
        return output;
    }
}
