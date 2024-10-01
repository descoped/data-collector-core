package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.error.ExecutionException;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.Execute;
import io.descoped.dc.api.node.Query;
import io.descoped.dc.core.executor.Executor;

import java.util.Map;

@Handler(forClass = Execute.class)
public class ExecuteHandler extends AbstractNodeHandler<Execute> {

    public ExecuteHandler(Execute node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        ExecutionContext executeTargetInput = ExecutionContext.of(super.execute(input));

        // process inputVariable
        for (Map.Entry<String, Query> inlineVariableEntry : node.inputVariable().entrySet()) {
            String inputVariableName = inlineVariableEntry.getKey();
            Query inputVariableQuery = inlineVariableEntry.getValue();

            // PageEntry is propagated by ParallelHandler; used to resolve Entry document. Or else, fallback to response content
            Object content = null;
            PageEntryState itemListItem = input.state(PageEntryState.class);
            Response response = input.state(Response.class);
            if (itemListItem != null) {
                content = itemListItem.nodeObject;

            } else if (response != null) {
                content = input.state(Response.class).body();
            }

            if (content != null) {
                // String inputVariableValue = Queries.from(input, inputVariableQuery).evaluateStringLiteral(content);
                // TODO inputVariable state aggregation needs to be verified wrt side effects
                String inputVariableValue = Queries.from(executeTargetInput, inputVariableQuery).evaluateStringLiteral(content);
                executeTargetInput.variables().put(inputVariableName, inputVariableValue);
            }
        }

        // validate required input variables
        for (String requiredInput : node.requiredInputs()) {
            if (!executeTargetInput.variables().containsKey(requiredInput)) {
                throw new ExecutionException(String.format("Required input variable: '%s' NOT found!", requiredInput));
            }
        }

        // execute node
        ExecutionContext output = Executor.execute(node.target(), executeTargetInput);
        return output;
    }
}
