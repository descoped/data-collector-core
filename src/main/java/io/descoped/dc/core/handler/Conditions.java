package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.node.Condition;
import io.descoped.dc.core.executor.Executor;
import io.descoped.dc.core.handler.state.ConditionType;

public class Conditions {

    public static boolean untilCondition(Condition condition, ExecutionContext context) {
        ExecutionContext untilConditionOutput = Executor.execute(condition, context);
        return untilConditionOutput.state(ConditionType.UNTIL_CONDITION_RESULT);
    }
}
